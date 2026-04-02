package ru.netology.nmedia.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostsAdapter
import ru.netology.nmedia.adapter.PostsLoadStateAdapter
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.viewmodel.PostViewModel

@AndroidEntryPoint
class FeedFragment : Fragment() {
    private val viewModel: PostViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentFeedBinding.inflate(inflater, container, false)

        val adapter = PostsAdapter(object : OnInteractionListener {
            override fun onEdit(post: Post) {
                viewModel.edit(post)
            }

            override fun onLike(post: Post) {
                viewModel.likeById(post.id)
            }

            override fun onRemove(post: Post) {
                viewModel.removeById(post.id)
            }

            override fun onShare(post: Post) {
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, post.content)
                    type = "text/plain"
                }

                val shareIntent =
                    Intent.createChooser(intent, getString(R.string.chooser_share_post))
                startActivity(shareIntent)
            }
        })

        binding.list.adapter = adapter.withLoadStateHeaderAndFooter(
            header = PostsLoadStateAdapter(),
            footer = PostsLoadStateAdapter(),
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.data.collectLatest(adapter::submitData)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            adapter.loadStateFlow.collectLatest { state ->
                val isInitialLoading =
                    state.refresh is LoadState.Loading && adapter.itemCount == 0
                val isRefreshing =
                    state.refresh is LoadState.Loading && adapter.itemCount > 0

                binding.progress.isVisible = isInitialLoading
                binding.swiperefresh.isRefreshing = isRefreshing
                binding.emptyText.isVisible =
                    state.refresh is LoadState.NotLoading && adapter.itemCount == 0

                val errorState = when {
                    state.refresh is LoadState.Error -> state.refresh as LoadState.Error
                    state.prepend is LoadState.Error -> state.prepend as LoadState.Error
                    state.append is LoadState.Error -> state.append as LoadState.Error
                    else -> null
                }

                errorState?.let {
                    Snackbar.make(binding.root, R.string.error_loading, Snackbar.LENGTH_LONG)
                        .setAction(R.string.retry_loading) { adapter.retry() }
                        .show()
                }
            }
        }

        binding.swiperefresh.setOnRefreshListener {
            adapter.refresh()
        }

        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.action_feedFragment_to_newPostFragment)
        }

        return binding.root
    }
}