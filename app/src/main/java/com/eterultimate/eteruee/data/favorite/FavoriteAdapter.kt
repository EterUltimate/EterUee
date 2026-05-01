package com.eterultimate.eteruee.data.favorite

import com.eterultimate.eteruee.data.db.entity.FavoriteEntity
import com.eterultimate.eteruee.data.model.FavoriteType

interface FavoriteAdapter<T> {
    val type: FavoriteType

    fun buildRefKey(target: T): String

    fun buildFavoriteEntity(
        target: T,
        existing: FavoriteEntity? = null,
        now: Long = System.currentTimeMillis()
    ): FavoriteEntity
}

