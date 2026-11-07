package com.eterultimate.eteruee.ai.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ImageEncodeCacheTest {
    @Test
    fun `same inputs produce the same key`() {
        val a = ImageEncodeCache.cacheKey("/tmp/a.jpg", 1000L, 2048L, 85)
        val b = ImageEncodeCache.cacheKey("/tmp/a.jpg", 1000L, 2048L, 85)
        assertEquals(a, b)
    }

    @Test
    fun `changed file metadata changes the key`() {
        val base = ImageEncodeCache.cacheKey("/tmp/a.jpg", 1000L, 2048L, 85)
        assertNotEquals(base, ImageEncodeCache.cacheKey("/tmp/b.jpg", 1000L, 2048L, 85))
        assertNotEquals(base, ImageEncodeCache.cacheKey("/tmp/a.jpg", 2000L, 2048L, 85))
        assertNotEquals(base, ImageEncodeCache.cacheKey("/tmp/a.jpg", 1000L, 4096L, 85))
        assertNotEquals(base, ImageEncodeCache.cacheKey("/tmp/a.jpg", 1000L, 2048L, 90))
    }
}
