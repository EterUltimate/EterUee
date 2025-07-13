package me.rerere.rikkahub.ui.pages.chat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.Image
import com.composables.icons.lucide.Lucide
import com.dokar.sonner.ToastType
import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.ai.ui.toSortedMessageParts
import me.rerere.ai.util.encodeBase64
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.model.Conversation
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.utils.toLocalString
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime

@Composable
fun ChatExportSheet(
  visible: Boolean,
  onDismissRequest: () -> Unit,
  conversation: Conversation,
  selectedMessages: List<UIMessage>
) {
  val context = LocalContext.current
  val toaster = LocalToaster.current

  if (visible) {
    ModalBottomSheet(
      onDismissRequest = onDismissRequest,
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(text = stringResource(id = R.string.chat_page_export_format))

        val markdownSuccessMessage =
          stringResource(id = R.string.chat_page_export_success, "Markdown")
        OutlinedCard(
          onClick = {
            exportToMarkdown(context, conversation, selectedMessages)
            toaster.show(
              markdownSuccessMessage,
              type = ToastType.Success
            )
            onDismissRequest()
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          ListItem(
            headlineContent = {
              Text(stringResource(id = R.string.chat_page_export_markdown))
            },
            supportingContent = {
              Text(stringResource(id = R.string.chat_page_export_markdown_desc))
            },
            leadingContent = {
              Icon(Lucide.FileText, contentDescription = null)
            }
          )
        }

        val imageSuccessMessage =
          stringResource(id = R.string.chat_page_export_success, "Image")
        OutlinedCard(
          onClick = {
            exportToImage(context, conversation, selectedMessages)
            toaster.show(
              imageSuccessMessage,
              type = ToastType.Success
            )
            onDismissRequest()
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          ListItem(
            headlineContent = {
              Text(stringResource(id = R.string.chat_page_export_image))
            },
            supportingContent = {
              Text(stringResource(id = R.string.chat_page_export_image_desc))
            },
            leadingContent = {
              Icon(Lucide.Image, contentDescription = null)
            }
          )
        }
      }
    }
  }
}

private fun exportToMarkdown(
  context: Context,
  conversation: Conversation,
  messages: List<UIMessage>
) {
  val filename = "chat-export-${LocalDateTime.now().toLocalString()}.md"

  val sb = buildAnnotatedString {
    append("# ${conversation.title}\n\n")
    append("*Exported on ${LocalDateTime.now().toLocalString()}*\n\n")

    messages.forEach { message ->
      val role = if (message.role == MessageRole.USER) "**User**" else "**Assistant**"
      append("$role:\n\n")
      message.parts.toSortedMessageParts().forEach { part ->
        when (part) {
          is UIMessagePart.Text -> {
            append(part.text)
            appendLine()
          }

          is UIMessagePart.Image -> {
            append("![Image](${part.encodeBase64().getOrNull()})")
            appendLine()
          }

          is UIMessagePart.Reasoning -> {
            part.reasoning.lines()
              .filter { it.isNotBlank() }
              .map { "> $it" }
              .forEach {
                append(it)
              }
            appendLine()
            appendLine()
          }

          else -> {}
        }
      }
      appendLine()
      append("---")
      appendLine()
    }
  }

  try {
    val dir = context.filesDir.resolve("temp")
    if (!dir.exists()) {
      dir.mkdirs()
    }
    val file = dir.resolve(filename)
    if (!file.exists()) {
      file.createNewFile()
    } else {
      file.delete()
      file.createNewFile()
    }
    FileOutputStream(file).use {
      it.write(sb.toString().toByteArray())
    }

    // Share the file
    val uri = FileProvider.getUriForFile(
      context,
      "${context.packageName}.fileprovider",
      file
    )
    shareFile(context, uri, "text/markdown")

  } catch (e: Exception) {
    e.printStackTrace()
  }
}

private fun exportToImage(
  context: Context,
  conversation: Conversation,
  messages: List<UIMessage>
) {
  val filename = "chat-export-${LocalDateTime.now().toLocalString()}.png"

  // Canvas settings
  val canvasWidth = 1080
  val padding = 60f
  val bubbleCornerRadius = 40f
  val bubblePaddingHorizontal = 30f
  val bubblePaddingVertical = 20f
  val messageSpacing = 30f

  val titleFontSize = 48f
  val contentFontSize = 32f
  val smallFontSize = 24f

  // Paint objects
  val titlePaint = Paint().apply {
    color = android.graphics.Color.BLACK
    textSize = titleFontSize
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    isAntiAlias = true
  }

  val contentPaint = TextPaint().apply {
    color = android.graphics.Color.BLACK
    textSize = contentFontSize
    typeface = Typeface.DEFAULT
    isAntiAlias = true
  }

  val smallPaint = Paint().apply {
    color = android.graphics.Color.GRAY
    textSize = smallFontSize
    typeface = Typeface.DEFAULT
    isAntiAlias = true
  }

  val userBubblePaint = Paint().apply {
    color = Color(0xFFE3F2FD).toArgb() // Light Blue
    isAntiAlias = true
    style = Paint.Style.FILL
  }

  val assistantBubblePaint = Paint().apply {
    color = Color(0xFFF5F5F5).toArgb() // Light Grey
    isAntiAlias = true
    style = Paint.Style.FILL
  }

  // --- Pass 1: Calculate height and prepare layouts ---
  val bubbleMaxWidth = (canvasWidth * 0.8f).toInt()

  val layouts = messages.map { message ->
    val text = message.parts.toSortedMessageParts().joinToString("\n") { part ->
      when (part) {
        is UIMessagePart.Text -> part.text
        is UIMessagePart.Image -> "[Image]"
        is UIMessagePart.Reasoning -> part.reasoning.lines().joinToString("\n") { "> $it" }
        else -> ""
      }
    }.ifBlank { " " }

    StaticLayout.Builder.obtain(text, 0, text.length, contentPaint, bubbleMaxWidth)
      .setAlignment(Layout.Alignment.ALIGN_NORMAL)
      .setLineSpacing(0f, 1.2f) // Increased line spacing for readability
      .setIncludePad(true)
      .build()
  }

  var totalHeight = padding * 2
  totalHeight += titleFontSize + 40
  totalHeight += smallFontSize + 60

  layouts.forEach { layout ->
    totalHeight += layout.height + bubblePaddingVertical * 2 + messageSpacing
  }
  totalHeight -= messageSpacing // No spacing after last message

  // Create bitmap and canvas
  val bitmap = createBitmap(canvasWidth, totalHeight.toInt())
  val canvas = Canvas(bitmap)

  // Background
  canvas.drawColor(android.graphics.Color.WHITE)

  // Draw App Logo
  try {
    ContextCompat.getDrawable(context, R.mipmap.ic_launcher_foreground)?.let { logoDrawable ->
      val logoSize = 120
      val logoBitmap = logoDrawable.toBitmap(logoSize, logoSize)
      val logoLeft = canvasWidth - padding - logoSize
      canvas.drawBitmap(logoBitmap, logoLeft, padding, null)
    }
  } catch (e: Exception) {
    // Some devices may not have a foreground launcher icon (e.g. Android TV)
    e.printStackTrace()
  }

  var currentY = padding

  // Draw title
  canvas.drawText(conversation.title, padding, currentY + titleFontSize, titlePaint)
  currentY += titleFontSize + 40

  // Draw export info
  val exportInfo = "${LocalDateTime.now().toLocalString()}  rikka-ai.com"
  canvas.drawText(exportInfo, padding, currentY + smallFontSize, smallPaint)
  currentY += smallFontSize + 60

  // --- Pass 2: Draw messages ---
  messages.forEachIndexed { index, message ->
    val layout = layouts[index]
    val bubbleHeight = layout.height + bubblePaddingVertical * 2

    var textMaxWidth = 0f
    for (i in 0 until layout.lineCount) {
      textMaxWidth = maxOf(textMaxWidth, layout.getLineWidth(i))
    }
    val bubbleWidth = textMaxWidth + bubblePaddingHorizontal * 2

    val bubbleRect: RectF
    if (message.role == MessageRole.USER) {
      val bubbleRight = canvasWidth - padding
      val bubbleLeft = bubbleRight - bubbleWidth
      bubbleRect = RectF(bubbleLeft, currentY, bubbleRight, currentY + bubbleHeight)
      canvas.drawRoundRect(bubbleRect, bubbleCornerRadius, bubbleCornerRadius, userBubblePaint)
    } else { // ASSISTANT, SYSTEM, TOOL
      val bubbleLeft = padding
      val bubbleRight = bubbleLeft + bubbleWidth
      bubbleRect = RectF(bubbleLeft, currentY, bubbleRight, currentY + bubbleHeight)
      canvas.drawRoundRect(
        bubbleRect,
        bubbleCornerRadius,
        bubbleCornerRadius,
        assistantBubblePaint
      )
    }

    canvas.save()
    canvas.translate(bubbleRect.left + bubblePaddingHorizontal, currentY + bubblePaddingVertical)
    layout.draw(canvas)
    canvas.restore()

    currentY += bubbleHeight + messageSpacing
  }

  try {
    val dir = context.filesDir.resolve("temp")
    if (!dir.exists()) {
      dir.mkdirs()
    }
    val file = dir.resolve(filename)
    if (!file.exists()) {
      file.createNewFile()
    } else {
      file.delete()
      file.createNewFile()
    }

    FileOutputStream(file).use { fos ->
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
    }

    // Share the file
    val uri = FileProvider.getUriForFile(
      context,
      "${context.packageName}.fileprovider",
      file
    )
    shareFile(context, uri, "image/png")
    Toast.makeText(context, "Exported", Toast.LENGTH_SHORT).show()
  } catch (e: Exception) {
    e.printStackTrace()
  } finally {
    bitmap.recycle()
  }
}

private fun shareFile(context: Context, uri: Uri, mimeType: String) {
  val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
    type = mimeType
    putExtra(android.content.Intent.EXTRA_STREAM, uri)
    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }
  context.startActivity(
    android.content.Intent.createChooser(
      intent,
      context.getString(R.string.chat_page_export_share_via)
    )
  )
}