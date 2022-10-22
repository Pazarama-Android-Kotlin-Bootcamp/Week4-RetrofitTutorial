package com.merttoptas.retrofittutorial.ui.posts.viewmodel

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.merttoptas.retrofittutorial.data.local.database.entity.PostEntity
import com.merttoptas.retrofittutorial.data.model.DataState
import com.merttoptas.retrofittutorial.data.model.Post
import com.merttoptas.retrofittutorial.data.model.PostDTO
import com.merttoptas.retrofittutorial.data.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

/**
 * Created by merttoptas on 15.10.2022.
 */

@HiltViewModel
class PostsViewModel @Inject constructor(
    private val postRepository: PostRepository,
) : ViewModel() {
    private var _postLiveData = MutableLiveData<DataState<List<PostDTO>?>>()
    val postLiveData: LiveData<DataState<List<PostDTO>?>>
        get() = _postLiveData

    private var _postCacheData = MutableLiveData<List<PostDTO>?>()
    private val postCacheData: LiveData<List<PostDTO>?>
        get() = _postCacheData

    private val _eventStateLiveData = MutableLiveData<PostViewEvent>()
    val eventStateLiveData: LiveData<PostViewEvent>
        get() = _eventStateLiveData

    init {
        getPosts()
    }

    private fun getPosts() {
        _postLiveData.postValue(DataState.Loading())
        Handler(Looper.getMainLooper()).postDelayed({
            postRepository.getPosts().enqueue(object : Callback<List<Post>> {
                override fun onResponse(call: Call<List<Post>>, response: Response<List<Post>>) {
                    if (response.isSuccessful) {
                        response.body()?.let {
                            val updatePostData = it.map { safePost ->
                                PostDTO(
                                    id = safePost.id,
                                    title = safePost.title,
                                    body = safePost.body,
                                    userId = safePost.userId,
                                    isFavorite = isExists(safePost.id)
                                )
                            }
                            _postLiveData.postValue(DataState.Success(updatePostData))
                            _postCacheData.value = updatePostData

                        } ?: kotlin.run {
                            _postLiveData.postValue(DataState.Error("Data Empty"))
                        }
                    } else {
                        _postLiveData.postValue(DataState.Error(response.message()))
                    }
                }

                override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                    _postLiveData.postValue(DataState.Error(t.message.toString()))
                    _eventStateLiveData.postValue(PostViewEvent.ShowMessage(t.message.toString()))
                }
            })
        }, 500)
    }

    fun onFavoritePost(post: PostDTO) {
        post.id?.let { safePostId ->
            postRepository.getPostById(safePostId)?.let {
                postRepository.deleteFavoritePost(
                    PostEntity(
                        postId = post.id.toString(),
                        postTitle = post.title,
                        postBody = post.body
                    )
                )
                _postLiveData.value =
                    DataState.Success(prepareUpdatePostData(post, isDelete = true))
            } ?: kotlin.run {
                postRepository.insertFavoritePost(
                    PostEntity(
                        postId = post.id.toString(),
                        postTitle = post.title,
                        postBody = post.body
                    )
                )
                _postLiveData.value = DataState.Success(prepareUpdatePostData(post))
            }
        }
    }

    private fun prepareUpdatePostData(post: PostDTO, isDelete: Boolean = false): List<PostDTO>? {
        postCacheData.value?.map {
            if (post.id == it.id) {
                it.copy(isFavorite = !isDelete)
            } else {
                it
            }
        }
        return postCacheData.value
    }

    private fun isExists(postId: Int?): Boolean {
        postId?.let {
            postRepository.getPostById(it)?.let {
                return true
            }
        }
        return false
    }
}

sealed class PostViewEvent {
    object NavigateToDetail : PostViewEvent()
    class ShowMessage(val message: String?) : PostViewEvent()
}