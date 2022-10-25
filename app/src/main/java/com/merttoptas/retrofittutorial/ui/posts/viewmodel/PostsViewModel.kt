package com.merttoptas.retrofittutorial.ui.posts.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.merttoptas.retrofittutorial.data.local.database.entity.PostEntity
import com.merttoptas.retrofittutorial.data.model.Post
import com.merttoptas.retrofittutorial.data.model.PostDTO
import com.merttoptas.retrofittutorial.data.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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

    private val _uiState = MutableStateFlow(PostsUiState())
    val uiState: StateFlow<PostsUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<PostViewEvent>()
    val uiEvent: SharedFlow<PostViewEvent> = _uiEvent

    init {
        getPosts()
    }

    private fun getPosts() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = PostsUiState(isLoading = true)
            delay(500)
            postRepository.getPosts().enqueue(object : Callback<List<Post>> {
                override fun onResponse(call: Call<List<Post>>, response: Response<List<Post>>) {
                    if (response.isSuccessful) {
                        response.body()?.let {
                            val updatePostData = it.map { safePost ->
                                PostDTO(
                                    id = safePost.id?.toInt(),
                                    title = safePost.title,
                                    body = safePost.body.toString(),
                                    userId = safePost.userId?.toInt(),
                                    isFavorite = runBlocking { isExists(safePost.id?.toInt()) }
                                )
                            }
                            _uiState.value =
                                uiState.value.copy(isLoading = false, posts = updatePostData)

                        } ?: kotlin.run {
                            _uiState.value = uiState.value.copy(error = "Error")
                        }
                    } else {
                        _uiState.value = uiState.value.copy(error = response.message())

                    }
                }

                override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                    _uiState.value = uiState.value.copy(error = t.message.toString())
                    viewModelScope.launch {
                        _uiEvent.emit(PostViewEvent.ShowMessage(t.message.toString()))
                    }
                }
            })
        }
    }

    suspend fun onFavoritePost(post: PostDTO) {
        post.id?.let { safePostId ->
            postRepository.getPostById(safePostId)?.let {
                postRepository.deleteFavoritePost(it)

                _uiState.value = uiState.value.copy(
                    posts = uiState.value.posts?.map { safePost ->
                        if (safePost.id == post.id) {
                            safePost.copy(isFavorite = false)
                        } else {
                            safePost
                        }
                    }
                )
            } ?: kotlin.run {
                postRepository.insertFavoritePost(
                    PostEntity(
                        postId = post.id.toString(),
                        postTitle = post.title,
                        postBody = post.body
                    )
                )
                _uiState.value = uiState.value.copy(
                    posts = uiState.value.posts?.map { safePost ->
                        if (safePost.id == post.id) {
                            safePost.copy(isFavorite = true)
                        } else {
                            safePost
                        }
                    }
                )
            }
        }
    }

    private suspend fun isExists(postId: Int?): Boolean {
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

data class PostsUiState(
    val isLoading: Boolean = false,
    val posts: List<PostDTO>? = null,
    val error: String? = null
)