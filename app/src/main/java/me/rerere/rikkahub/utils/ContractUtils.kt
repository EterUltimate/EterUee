package me.rerere.rikkahub.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.CallSuper

open class GetContentWithMultiMime : ActivityResultContract<List<String>, Uri?>() {
  @CallSuper
  override fun createIntent(context: Context, input: List<String>): Intent {
    return Intent(Intent.ACTION_GET_CONTENT)
      .addCategory(Intent.CATEGORY_OPENABLE)
      .setType("*/*")
      .putExtra(Intent.EXTRA_MIME_TYPES, input.toTypedArray())
  }

  final override fun getSynchronousResult(
    context: Context,
    input: List<String>
  ): SynchronousResult<Uri?>? = null

  final override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
    return intent.takeIf { resultCode == Activity.RESULT_OK }?.data
  }
}