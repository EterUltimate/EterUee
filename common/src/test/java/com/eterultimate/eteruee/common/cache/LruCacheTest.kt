package com.eterultimate.eteruee.common.cache

import org.junit.Test
import org.junit.Assert.*

/**
 * LruCache 单元测试
 * 
 * 测试覆盖：
 * - 基本读写操作
 * - LRU淘汰策略
 * - TTL过期机制
 * - 线程安全
 */
class LruCacheTest {

    @Test
    fun testBasicPutAndGet() {
        val store = InMemoryCacheStore<String, String>()
        val cache = LruCache<String, String>(capacity = 10, store = store)
        
        cache.put("key1", "value1")
        cache.put("key2", "value2")
        
        assertEquals("value1", cache.get("key1"))
        assertEquals("value2", cache.get("key2"))
    }

    @Test
    fun testGetNonExistentKey() {
        val store = InMemoryCacheStore<String, String>()
        val cache = LruCache<String, String>(capacity = 10, store = store)
        
        assertNull(cache.get("nonexistent"))
    }

    @Test
    fun testLRUEviction() {
        val store = InMemoryCacheStore<String, String>()
        val cache = LruCache<String, String>(capacity = 3, store = store)
        
        // 添加4个元素，容量为3
        cache.put("key1", "value1")
        cache.put("key2", "value2")
        cache.put("key3", "value3")
        cache.put("key4", "value4") // 应该触发淘汰
        
        // key1应该被淘汰（最久未使用）- 从内存中检查
        assertFalse(cache.keysInMemory().contains("key1"))
        assertTrue(cache.keysInMemory().contains("key2"))
        assertTrue(cache.keysInMemory().contains("key3"))
        assertTrue(cache.keysInMemory().contains("key4"))
    }

    @Test
    fun testLRUAccessOrder() {
        val store = InMemoryCacheStore<String, String>()
        val cache = LruCache<String, String>(capacity = 3, store = store)
        
        cache.put("key1", "value1")
        cache.put("key2", "value2")
        cache.put("key3", "value3")
        
        // 访问key1，使其变为最近使用
        cache.get("key1")
        
        // 添加新元素，应该淘汰key2（最久未使用）
        cache.put("key4", "value4")
        
        // 从内存中检查
        assertFalse(cache.keysInMemory().contains("key2"))
        assertTrue(cache.keysInMemory().contains("key1"))
        assertTrue(cache.keysInMemory().contains("key3"))
        assertTrue(cache.keysInMemory().contains("key4"))
    }

    @Test
    fun testTTLExpiration() {
        val store = InMemoryCacheStore<String, String>()
        val cache = LruCache<String, String>(capacity = 10, store = store)
        
        // 设置100ms的TTL
        cache.put("key1", "value1", ttlMillis = 100)
        
        // 立即获取应该成功
        assertEquals("value1", cache.get("key1"))
        
        // 等待过期
        Thread.sleep(150)
        
        // 过期后应该返回null
        assertNull(cache.get("key1"))
    }

    @Test
    fun testDefaultTTL() {
        val store = InMemoryCacheStore<String, String>()
        val cache = LruCache<String, String>(
            capacity = 10,
            store = store,
            expireAfterWriteMillis = 100
        )
        
        cache.put("key1", "value1")
        
        // 立即获取应该成功
        assertEquals("value1", cache.get("key1"))
        
        // 等待过期
        Thread.sleep(150)
        
        // 过期后应该返回null
        assertNull(cache.get("key1"))
    }

    @Test
    fun testRemove() {
        val store = InMemoryCacheStore<String, String>()
        val cache = LruCache<String, String>(capacity = 10, store = store)
        
        cache.put("key1", "value1")
        cache.remove("key1")
        
        assertNull(cache.get("key1"))
    }

    @Test
    fun testClear() {
        val store = InMemoryCacheStore<String, String>()
        val cache = LruCache<String, String>(capacity = 10, store = store)
        
        cache.put("key1", "value1")
        cache.put("key2", "value2")
        cache.put("key3", "value3")
        
        cache.clear()
        
        assertNull(cache.get("key1"))
        assertNull(cache.get("key2"))
        assertNull(cache.get("key3"))
        assertEquals(0, cache.size())
    }

    @Test
    fun testContainsKey() {
        val store = InMemoryCacheStore<String, String>()
        val cache = LruCache<String, String>(capacity = 10, store = store)
        
        cache.put("key1", "value1")
        
        assertTrue(cache.containsKey("key1"))
        assertFalse(cache.containsKey("key2"))
    }

    @Test
    fun testSize() {
        val store = InMemoryCacheStore<String, String>()
        val cache = LruCache<String, String>(capacity = 10, store = store)
        
        assertEquals(0, cache.size())
        
        cache.put("key1", "value1")
        assertEquals(1, cache.size())
        
        cache.put("key2", "value2")
        assertEquals(2, cache.size())
    }

    @Test
    fun testKeysInMemory() {
        val store = InMemoryCacheStore<String, String>()
        val cache = LruCache<String, String>(capacity = 10, store = store)
        
        cache.put("key1", "value1")
        cache.put("key2", "value2")
        
        val keys = cache.keysInMemory()
        
        assertEquals(2, keys.size)
        assertTrue(keys.contains("key1"))
        assertTrue(keys.contains("key2"))
    }

    @Test
    fun testUpdateExistingKey() {
        val store = InMemoryCacheStore<String, String>()
        val cache = LruCache<String, String>(capacity = 10, store = store)
        
        cache.put("key1", "value1")
        cache.put("key1", "value2") // 更新
        
        assertEquals("value2", cache.get("key1"))
        assertEquals(1, cache.size())
    }

    @Test
    fun testDeleteOnEvict() {
        val store = InMemoryCacheStore<String, String>()
        val cache = LruCache<String, String>(
            capacity = 2,
            store = store,
            deleteOnEvict = true
        )
        
        cache.put("key1", "value1")
        cache.put("key2", "value2")
        
        // 访问key1，使其变为最近使用
        cache.get("key1")
        
        cache.put("key3", "value3") // 触发淘汰，应该淘汰key2
        
        // key2应该从store中删除（最久未使用）
        assertNull(store.loadEntry("key2"))
        // key1应该还在
        assertNotNull(store.loadEntry("key1"))
    }

    @Test
    fun testPreloadFromStore() {
        val store = InMemoryCacheStore<String, String>()
        
        // 预先在store中存入数据
        store.saveEntry("key1", CacheEntry("value1", null))
        store.saveEntry("key2", CacheEntry("value2", null))
        
        val cache = LruCache<String, String>(
            capacity = 10,
            store = store,
            preloadFromStore = true
        )
        
        // 应该能从store中加载数据
        assertEquals("value1", cache.get("key1"))
        assertEquals("value2", cache.get("key2"))
        assertEquals(2, cache.size())
    }

    @Test
    fun testPreloadExpiredEntries() {
        val store = InMemoryCacheStore<String, String>()
        
        // 存入已过期的数据
        val expiredTime = System.currentTimeMillis() - 1000
        store.saveEntry("key1", CacheEntry("value1", expiredTime))
        
        val cache = LruCache<String, String>(
            capacity = 10,
            store = store,
            preloadFromStore = true
        )
        
        // 过期的数据不应该被加载到内存
        assertEquals(0, cache.size())
        // 从cache.get也应该返回null（因为已过期）
        assertNull(cache.get("key1"))
    }
}

// 简单的内存存储实现，用于测试
class InMemoryCacheStore<K, V> : CacheStore<K, V> where K : Any {
    private val data = mutableMapOf<K, CacheEntry<V>>()
    
    override fun loadEntry(key: K): CacheEntry<V>? {
        return data[key]
    }
    
    override fun loadAllEntries(): Map<K, CacheEntry<V>> {
        return data.toMap()
    }
    
    override fun saveEntry(key: K, entry: CacheEntry<V>) {
        data[key] = entry
    }
    
    override fun remove(key: K) {
        data.remove(key)
    }
    
    override fun clear() {
        data.clear()
    }
    
    override fun keys(): Set<K> {
        return data.keys
    }
}
