package com.example.c001apk.ui.search

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.absinthe.libraries.utils.extensions.dp
import com.example.c001apk.adapter.AppAdapter
import com.example.c001apk.adapter.FooterAdapter
import com.example.c001apk.adapter.HeaderAdapter
import com.example.c001apk.databinding.FragmentSearchFeedBinding
import com.example.c001apk.ui.base.BaseFragment
import com.example.c001apk.ui.home.IOnTabClickContainer
import com.example.c001apk.ui.home.IOnTabClickListener
import com.example.c001apk.util.Utils.getColorFromAttr
import com.example.c001apk.view.LinearItemDecoration
import com.example.c001apk.view.StaggerItemDecoration

class SearchContentFragment : BaseFragment<FragmentSearchFeedBinding>(),
    IOnSearchMenuClickListener, IOnTabClickListener {

    private val viewModel by lazy { ViewModelProvider(this)[SearchContentViewModel::class.java] }
    private lateinit var mAdapter: AppAdapter
    private lateinit var footerAdapter: FooterAdapter
    private lateinit var mLayoutManager: LinearLayoutManager
    private lateinit var sLayoutManager: StaggeredGridLayoutManager

    companion object {
        @JvmStatic
        fun newInstance(keyWord: String, type: String, pageType: String?, pageParam: String?) =
            SearchContentFragment().apply {
                arguments = Bundle().apply {
                    putString("KEYWORD", keyWord)
                    putString("TYPE", type)
                    putString("pageType", pageType)
                    putString("pageParam", pageParam)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            viewModel.keyWord = it.getString("KEYWORD")
            viewModel.type = it.getString("TYPE")
            viewModel.pageType = it.getString("pageType")
            viewModel.pageParam = it.getString("pageParam")
        }
    }

    override fun onPause() {
        super.onPause()

        detachLift()

        (requireParentFragment() as? IOnSearchMenuClickContainer)?.controller = null
        (requireParentFragment() as? IOnTabClickContainer)?.tabController = null
    }

    override fun onResume() {
        super.onResume()

        (requireParentFragment() as? IOnSearchMenuClickContainer)?.controller = this
        (requireParentFragment() as? IOnTabClickContainer)?.tabController = this

        if (viewModel.isInit) {
            viewModel.isInit = false
            initView()
            initData()
            initRefresh()
            initScroll()
            initObserve()
        }

        initLift()

    }

    override fun onStart() {
        super.onStart()
        initLift()
    }

    override fun onStop() {
        super.onStop()
        detachLift()
    }

    private fun detachLift() {
        binding.recyclerView.borderViewDelegate.borderVisibilityChangedListener = null
    }

    private fun initLift() {
        val parent = requireParentFragment() as SearchResultFragment
        parent.binding.appBar.setLifted(
            !binding.recyclerView.borderViewDelegate.isShowingTopBorder
        )
        binding.recyclerView.borderViewDelegate
            .setBorderVisibilityChangedListener { top, _, _, _ ->
                parent.binding.appBar.setLifted(!top)
            }
    }

    private fun initObserve() {
        viewModel.afterFollow.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandledOrReturnNull()?.let {
                mAdapter.notifyItemChanged(it)
            }
        }

        viewModel.toastText.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandledOrReturnNull()?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.changeState.observe(viewLifecycleOwner) {
            footerAdapter.setLoadState(it.first, it.second)
            footerAdapter.notifyItemChanged(0)
            if (it.first != FooterAdapter.LoadState.LOADING) {
                binding.swipeRefresh.isRefreshing = false
                binding.indicator.parent.isIndeterminate = false
                binding.indicator.parent.visibility = View.GONE
                viewModel.isLoadMore = false
                viewModel.isRefreshing = false
            }
        }

        viewModel.searchData.observe(viewLifecycleOwner) {
            viewModel.listSize = it.size
            mAdapter.submitList(it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!viewModel.isInit) {
            initView()
            initData()
            initRefresh()
            initScroll()
            initObserve()
        }

        /*
                viewModel.postFollowUnFollowData.observe(viewLifecycleOwner) { result ->
                    if (viewModel.postFollowUnFollow) {
                        viewModel.postFollowUnFollow = false

                        val response = result.getOrNull()
                        if (response != null) {
                            if (viewModel.followType) {
                                viewModel.searchList[viewModel.position].isFollow = 0
                            } else {
                                viewModel.searchList[viewModel.position].isFollow = 1
                            }
                            mAdapter.notifyItemChanged(viewModel.position)
                        } else {
                            result.exceptionOrNull()?.printStackTrace()
                        }
                    }
                }*/

    }

    private fun initScroll() {
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {

                    if (viewModel.listSize != -1 && isAdded)
                        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                            viewModel.lastVisibleItemPosition =
                                mLayoutManager.findLastVisibleItemPosition()
                        } else {
                            val positions = sLayoutManager.findLastVisibleItemPositions(null)
                            viewModel.lastVisibleItemPosition = positions[0]
                            for (pos in positions) {
                                if (pos > viewModel.lastVisibleItemPosition) {
                                    viewModel.lastVisibleItemPosition = pos
                                }
                            }
                        }

                    if (viewModel.lastVisibleItemPosition == viewModel.listSize + 1
                        && !viewModel.isEnd && !viewModel.isRefreshing && !viewModel.isLoadMore
                    ) {
                        viewModel.page++
                        loadMore()
                    }
                }
            }
        })
    }

    private fun loadMore() {
        viewModel.isLoadMore = true
        viewModel.fetchSearchData()
    }

    private fun initRefresh() {
        binding.swipeRefresh.setColorSchemeColors(
            requireContext().getColorFromAttr(
                rikka.preference.simplemenu.R.attr.colorPrimary
            )
        )
        binding.swipeRefresh.setOnRefreshListener {
            binding.indicator.parent.isIndeterminate = false
            binding.indicator.parent.visibility = View.GONE
            refreshData()
        }
    }

    private fun initData() {
        if (viewModel.listSize == -1) {
            binding.indicator.parent.visibility = View.VISIBLE
            binding.indicator.parent.isIndeterminate = true
            refreshData()
        }
    }

    private fun refreshData() {
        viewModel.lastVisibleItemPosition = 0
        viewModel.lastItem = null
        viewModel.page = 1
        viewModel.isEnd = false
        viewModel.isRefreshing = true
        viewModel.isLoadMore = false
        viewModel.fetchSearchData()
    }

    private fun initView() {
        mAdapter = AppAdapter(viewModel.ItemClickListener())
        footerAdapter = FooterAdapter(ReloadListener())
        binding.recyclerView.apply {
            adapter = ConcatAdapter(HeaderAdapter(), mAdapter, footerAdapter)
            layoutManager =
                if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    mLayoutManager = LinearLayoutManager(requireContext())
                    mLayoutManager
                } else {
                    sLayoutManager =
                        StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
                    sLayoutManager
                }
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                addItemDecoration(LinearItemDecoration(10.dp))
            else
                addItemDecoration(StaggerItemDecoration(10.dp))
        }
    }


    override fun onSearch(type: String, value: String, id: String?) {
        when (type) {
            "sort" -> viewModel.sort = value
            "feedType" -> viewModel.feedType = value
        }
        viewModel.searchData.postValue(emptyList())
        binding.indicator.parent.visibility = View.VISIBLE
        binding.indicator.parent.isIndeterminate = true
        refreshData()
    }

    override fun onReturnTop(isRefresh: Boolean?) {
        binding.swipeRefresh.isRefreshing = true
        binding.recyclerView.scrollToPosition(0)
        refreshData()
    }

    inner class ReloadListener : FooterAdapter.FooterListener {
        override fun onReLoad() {
            loadMore()
        }
    }

}