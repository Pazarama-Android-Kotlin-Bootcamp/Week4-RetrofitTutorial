package com.merttoptas.retrofittutorial.data.repository

import com.merttoptas.retrofittutorial.data.local.database.entity.PostEntity
import com.merttoptas.retrofittutorial.data.model.Post
import io.reactivex.rxjava3.core.Observable
import retrofit2.Call

/**
 * Created by merttoptas on 16.10.2022.
 */

interface PostRepository {
    fun getPosts(): Observable<List<Post>>
    fun getPostById(id: Int): PostEntity?
    fun insertFavoritePost(post: PostEntity)
    fun deleteFavoritePost(post: PostEntity)
}