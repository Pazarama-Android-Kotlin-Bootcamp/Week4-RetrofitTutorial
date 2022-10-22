package com.merttoptas.retrofittutorial.data.repository

import com.merttoptas.retrofittutorial.data.local.database.entity.PostEntity
import com.merttoptas.retrofittutorial.data.model.Post
import retrofit2.Call

/**
 * Created by merttoptas on 16.10.2022.
 */

interface PostRepository {
   suspend fun getPosts(): Call<List<Post>>
   suspend fun getPostById(id: Int): PostEntity?
   suspend fun insertFavoritePost(post: PostEntity)
   suspend fun deleteFavoritePost(post: PostEntity)
}