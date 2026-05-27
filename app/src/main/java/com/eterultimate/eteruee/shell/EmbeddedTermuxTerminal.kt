package com.eterultimate.eteruee.shell

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient

private const val TAG = "EmbeddedTermuxTerminal"

class EmbeddedTermuxTerminalClient(
    private val context: Context,
    private val onTitle: (String) -> Unit,
    private val onFinished: () -> Unit,
) : TerminalSessionClient, TerminalViewClient {
    var terminalView: TerminalView? = null
    var terminalSession: TerminalSession? = null

    override fun onTextChanged(changedSession: TerminalSession) {
        if (changedSession == terminalSession) {
            terminalView?.onScreenUpdated()
        }
    }

    override fun onTitleChanged(changedSession: TerminalSession) {
        if (changedSession == terminalSession) {
            onTitle(changedSession.title ?: "")
        }
    }

    override fun onSessionFinished(finishedSession: TerminalSession) {
        if (finishedSession == terminalSession) {
            terminalView?.onScreenUpdated()
            onFinished()
        }
    }

    override fun onCopyTextToClipboard(session: TerminalSession, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Terminal", text))
    }

    override fun onPasteTextFromClipboard(session: TerminalSession?) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val text = clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
        if (!text.isNullOrEmpty()) {
            (session ?: terminalSession)?.write(text)
        }
    }

    override fun onBell(session: TerminalSession) = Unit

    override fun onColorsChanged(session: TerminalSession) {
        if (session == terminalSession) {
            terminalView?.onScreenUpdated()
        }
    }

    override fun onTerminalCursorStateChange(state: Boolean) {
        terminalView?.setTerminalCursorBlinkerState(state, true)
    }

    override fun setTerminalShellPid(session: TerminalSession, pid: Int) {
        Log.d(TAG, "Started embedded shell pid=$pid")
    }

    override fun getTerminalCursorStyle(): Int {
        return TerminalEmulator.DEFAULT_TERMINAL_CURSOR_STYLE
    }

    override fun onScale(scale: Float): Float {
        return scale.coerceIn(0.75f, 2.0f)
    }

    override fun onSingleTapUp(e: MotionEvent) {
        val view = terminalView ?: return
        view.requestFocus()
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        inputMethodManager?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    override fun shouldBackButtonBeMappedToEscape() = false

    override fun shouldEnforceCharBasedInput() = false

    override fun shouldUseCtrlSpaceWorkaround() = false

    override fun isTerminalViewSelected() = true

    override fun copyModeChanged(copyMode: Boolean) = Unit

    override fun onKeyDown(keyCode: Int, e: KeyEvent, session: TerminalSession) = false

    override fun onKeyUp(keyCode: Int, e: KeyEvent) = false

    override fun onLongPress(event: MotionEvent) = false

    override fun readControlKey() = false

    override fun readAltKey() = false

    override fun readShiftKey() = false

    override fun readFnKey() = false

    override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession) = false

    override fun onEmulatorSet() {
        terminalView?.setTerminalCursorBlinkerRate(700)
        terminalView?.setTerminalCursorBlinkerState(true, true)
    }

    override fun logError(tag: String, message: String) {
        Log.e(tag, message)
    }

    override fun logWarn(tag: String, message: String) {
        Log.w(tag, message)
    }

    override fun logInfo(tag: String, message: String) {
        Log.i(tag, message)
    }

    override fun logDebug(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun logVerbose(tag: String, message: String) {
        Log.v(tag, message)
    }

    override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) {
        Log.e(tag, message, e)
    }

    override fun logStackTrace(tag: String, e: Exception) {
        Log.e(tag, "Terminal error", e)
    }
}

fun createEmbeddedTermuxSession(
    context: Context,
    client: EmbeddedTermuxTerminalClient,
): TerminalSession {
    val cwd = LocalShellRunner.defaultWorkingDir(context).apply {
        if (!exists()) mkdirs()
    }
    return TerminalSession(
        LocalShellRunner.SHELL_PATH,
        cwd.absolutePath,
        arrayOf("sh"),
        LocalShellRunner.environmentArray(context),
        2000,
        client,
    ).also {
        it.mSessionName = "EterUee Shell"
        client.terminalSession = it
    }
}
