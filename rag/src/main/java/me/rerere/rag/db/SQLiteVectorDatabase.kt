package me.rerere.rag.db

import android.content.Context
import android.util.Log
import io.requery.android.database.sqlite.SQLiteCustomExtension
import io.requery.android.database.sqlite.SQLiteDatabase
import io.requery.android.database.sqlite.SQLiteDatabaseConfiguration
import io.requery.android.database.sqlite.SQLiteOpenHelper
import me.rerere.rag.document.Document
import org.json.JSONObject

private const val TAG = "VectorDatabase"
private const val DB_NAME = "rag.db"
private const val DB_VERSION = 1

class SQLiteVectorDatabase(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION), VectorDatabase {

    override fun createConfiguration(path: String?, openFlags: Int): SQLiteDatabaseConfiguration? {
        val configuration = SQLiteDatabaseConfiguration(path, openFlags)
        // 加载SQLite向量扩展
        configuration.customExtensions.add(
            SQLiteCustomExtension(
                "vec0",
                "sqlite3_vec_init"
            )
        )
        return configuration
    }

    fun createDatabase(name: String, dimensions: Int) {
        Log.d(TAG, "Creating vector database tables")
        // 创建向量表
        writableDatabase.execSQL(
            """
            CREATE VIRTUAL TABLE $name USING vec0(
              id TEXT PRIMARY KEY,
              embedding FLOAT[${dimensions}] distance_metric=cosine,
              content TEXT,            
              metadata TEXT            
            );
        """.trimIndent()
        )
    }

    override fun onCreate(db: SQLiteDatabase) {

    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        error("Upgrade not supported")
    }

    override fun addDocuments(
        table: String,
        document: List<Document>,
        vectors: List<FloatArray>
    ) {
        if (document.size != vectors.size) {
            throw IllegalArgumentException("Document list and vector list must have the same size")
        }

        val db = writableDatabase
        db.beginTransaction()

        try {
            for (i in document.indices) {
                val doc = document[i]
                val vector = vectors[i]

                // 将向量转换为字符串表示，格式为 [a, b, c, ...]
                val vectorStr = vector.joinToString(prefix = "[", postfix = "]", separator = ", ")

                // 将元数据转换为JSON字符串
                val metadataJson = JSONObject(doc.metadata).toString()

                // 插入文档和向量
                db.execSQL(
                    """
                    INSERT INTO $table (id, embedding, content, metadata)
                    VALUES (?, $vectorStr, ?, ?)
                    """,
                    arrayOf(doc.id, doc.content, metadataJson)
                )
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e(TAG, "Error adding documents: ${e.message}", e)
        } finally {
            db.endTransaction()
        }
    }

    override fun search(table: String, embedding: FloatArray, limit: Int): List<Document> {
        val results = mutableListOf<Document>()
        val db = readableDatabase

        // 将向量转换为字符串表示
        val vectorStr = embedding.joinToString(prefix = "[", postfix = "]", separator = ", ")

        try {
            // 使用向量匹配查询，按距离排序
            val cursor = db.rawQuery(
                """
                SELECT id, content, metadata, distance
                FROM $table
                WHERE embedding MATCH ?
                ORDER BY distance
                LIMIT ?
                """,
                arrayOf(vectorStr, limit.toString())
            )

            cursor.use {
                val idColumnIndex = it.getColumnIndexOrThrow("id")
                val contentColumnIndex = it.getColumnIndexOrThrow("content")
                val metadataColumnIndex = it.getColumnIndexOrThrow("metadata")

                while (it.moveToNext()) {
                    val id = it.getString(idColumnIndex)
                    val content = it.getString(contentColumnIndex)
                    val metadataStr = it.getString(metadataColumnIndex)

                    // 将JSON字符串转换回Map
                    val metadata = parseMetadataJson(metadataStr)

                    results.add(Document(id, content, metadata))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching documents: ${e.message}", e)
        }

        return results
    }

    override fun deleteDocument(table: String, id: String): Boolean {
        val db = writableDatabase
        try {
            val deletedRows = db.delete(table, "id = ?", arrayOf(id))
            return deletedRows > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting document: ${e.message}", e)
            return false
        }
    }

    override fun getAllDocuments(table: String): List<Document> {
        val results = mutableListOf<Document>()
        val db = readableDatabase

        try {
            val cursor = db.query(
                table,
                arrayOf("id", "content", "metadata"),
                null,
                null,
                null,
                null,
                null
            )

            cursor.use {
                val idColumnIndex = it.getColumnIndexOrThrow("id")
                val contentColumnIndex = it.getColumnIndexOrThrow("content")
                val metadataColumnIndex = it.getColumnIndexOrThrow("metadata")

                while (it.moveToNext()) {
                    val id = it.getString(idColumnIndex)
                    val content = it.getString(contentColumnIndex)
                    val metadataStr = it.getString(metadataColumnIndex)

                    // 将JSON字符串转换回Map
                    val metadata = parseMetadataJson(metadataStr)

                    results.add(Document(id, content, metadata))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all documents: ${e.message}", e)
        }

        return results
    }

    private fun parseMetadataJson(metadataJson: String): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        try {
            val jsonObject = JSONObject(metadataJson)
            val keys = jsonObject.keys()

            while (keys.hasNext()) {
                val key = keys.next()
                result[key] = jsonObject.get(key)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing metadata JSON: ${e.message}", e)
        }
        return result
    }

    override fun clear(table: String) {
        val db = writableDatabase
        try {
            db.execSQL("DELETE FROM $table")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing database: ${e.message}", e)
        }
    }

    override fun close() {
        super.close()
    }
}