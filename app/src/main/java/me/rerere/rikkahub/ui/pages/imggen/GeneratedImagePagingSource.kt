package me.rerere.rikkahub.ui.pages.imggen

import android.app.Application
import androidx.paging.PagingSource
import androidx.paging.PagingState
import me.rerere.rikkahub.data.repository.GenMediaRepository
import me.rerere.rikkahub.utils.getImagesDir
import java.io.File

class GeneratedImagePagingSource(
    private val repository: GenMediaRepository,
    private val context: Application
) : PagingSource<Int, GeneratedImage>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GeneratedImage> {
        return try {
            val page = params.key ?: 0
            val pageSize = params.loadSize
            val offset = page * pageSize
            
            val imagesDir = context.getImagesDir()
            val entities = repository.getAllMediaList()
            
            // Convert entities to GeneratedImage, checking if files exist
            val images = entities.drop(offset).take(pageSize).mapNotNull { entity ->
                val file = File(imagesDir, entity.path.substringAfterLast("/"))
                if (file.exists()) {
                    GeneratedImage(
                        id = entity.id,
                        prompt = entity.prompt,
                        filePath = file.absolutePath,
                        timestamp = entity.createAt,
                        model = entity.modelId
                    )
                } else {
                    // Clean up orphaned database entries
                    try {
                        repository.deleteMedia(entity.id)
                    } catch (e: Exception) {
                        // Log but don't fail the whole operation
                    }
                    null
                }
            }
            
            LoadResult.Page(
                data = images,
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (images.size < pageSize) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, GeneratedImage>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}