package me.rerere.rikkahub.data.ai

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import me.rerere.rikkahub.data.datastore.SettingsStore

private const val TAG = "GenerationWorker"

class GenerationWorker(
    appContext: Context,
    params: WorkerParameters,
    val settingsStore: SettingsStore
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
//        val settings =
//            JsonInstant.decodeFromString<Settings>(checkNotNull(inputData.getString("settings")))
//        val model =
//            JsonInstant.decodeFromString<Model>(checkNotNull(inputData.getString("model")))
//        val messages =
//            JsonInstant.decodeFromString<List<UIMessage>>(checkNotNull(inputData.getString("messages")))
        Log.i(TAG, "doWork: hi")

        return Result.success()
    }
}