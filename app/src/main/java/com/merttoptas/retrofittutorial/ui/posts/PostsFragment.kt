package com.merttoptas.retrofittutorial.ui.posts

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.merttoptas.retrofittutorial.data.local.database.PostsDatabase
import com.merttoptas.retrofittutorial.data.model.DataState
import com.merttoptas.retrofittutorial.data.model.Post
import com.merttoptas.retrofittutorial.data.model.PostDTO
import com.merttoptas.retrofittutorial.data.repository.PostRepositoryImpl
import com.merttoptas.retrofittutorial.databinding.FragmentPostsBinding
import com.merttoptas.retrofittutorial.ui.loadingprogress.LoadingProgressBar
import com.merttoptas.retrofittutorial.ui.posts.adapter.OnPostClickListener
import com.merttoptas.retrofittutorial.ui.posts.adapter.PostsAdapter
import com.merttoptas.retrofittutorial.ui.posts.viewmodel.PostViewEvent
import com.merttoptas.retrofittutorial.ui.posts.viewmodel.PostViewModelFactory
import com.merttoptas.retrofittutorial.ui.posts.viewmodel.PostsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PostsFragment : Fragment(), OnPostClickListener {
    lateinit var loadingProgressBar: LoadingProgressBar
    private lateinit var binding: FragmentPostsBinding
    private val viewModel by viewModels<PostsViewModel>()
    private val job = Job()
    private val coroutineJob = CoroutineScope(Dispatchers.Main + job)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentPostsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadingProgressBar = LoadingProgressBar(requireContext())

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        lifecycleScope.launchWhenResumed {
            launch {
                viewModel.uiState.collect { uiState ->
                    uiState.posts?.let {
                        binding.rvPostsList.adapter = PostsAdapter(this@PostsFragment).apply {
                            submitList(it)
                        }
                        if (uiState.isLoading) loadingProgressBar.show() else loadingProgressBar.hide()
                    }
                }
            }

            launch {
                viewModel.uiEvent.collect { uiEvent ->
                    when (uiEvent) {
                        is PostViewEvent.ShowMessage -> {
                            loadingProgressBar.hide()
                            Toast.makeText(requireContext(), uiEvent.message, Toast.LENGTH_SHORT).show()
                        }
                        is PostViewEvent.NavigateToDetail -> {
                        }
                        else -> {}
                    }
                }
            }
        }


        /*
        Way 2
         viewModel.postLiveData.observe(viewLifecycleOwner) {
            binding.rvPostsList.adapter = PostsAdapter().apply {
                submitList(it)
            }
        }

         */
    }

    override fun onPostClick(post: PostDTO) {
        coroutineJob.launch {
            viewModel.onFavoritePost(post)
        }
    }
}

/*
@BindingAdapter("app:postList")
fun setPostList(recyclerView: RecyclerView, postList: List<Post>?) {
    postList?.let {
        recyclerView.adapter = PostsAdapter().apply {
            submitList(it)
        }
    }
}
 */
