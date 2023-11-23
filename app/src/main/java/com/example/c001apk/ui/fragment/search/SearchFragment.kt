package com.example.c001apk.ui.fragment.search

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.ThemeUtils
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import com.example.c001apk.R
import com.example.c001apk.adapter.HistoryAdapter
import com.example.c001apk.databinding.FragmentSearchBinding
import com.example.c001apk.logic.database.SearchHistoryDatabase
import com.example.c001apk.logic.model.SearchHistory
import com.example.c001apk.ui.fragment.minterface.IOnItemClickListener
import com.example.c001apk.util.PrefManager
import com.example.c001apk.viewmodel.AppViewModel
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.concurrent.thread

class SearchFragment : Fragment(), IOnItemClickListener {

    private lateinit var binding: FragmentSearchBinding
    private val viewModel by lazy { ViewModelProvider(this)[AppViewModel::class.java] }
    private lateinit var mAdapter: HistoryAdapter
    private lateinit var mLayoutManager: FlexboxLayoutManager
    private val searchHistoryDao by lazy {
        SearchHistoryDatabase.getDatabase(this@SearchFragment.requireContext()).searchHistoryDao()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            viewModel.pageType = it.getString("pageType")!!
            viewModel.pageParam = it.getString("pageParam")!!
            viewModel.title = it.getString("title")!!
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(pageType: String, pageParam: String, title: String) =
            SearchFragment().apply {
                arguments = Bundle().apply {
                    putString("pageType", pageType)
                    putString("pageParam", pageParam)
                    putString("title", title)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (viewModel.historyList.isEmpty())
            binding.historyLayout.visibility = View.GONE
        else
            binding.historyLayout.visibility = View.VISIBLE

        initView()
        if (viewModel.historyList.isEmpty())
            queryData()

        initEditText()
        initEdit()
        initButton()
        initClearHistory()

    }

    private fun initView() {
        mLayoutManager = FlexboxLayoutManager(activity)
        mLayoutManager.flexDirection = FlexDirection.ROW
        mLayoutManager.flexWrap = FlexWrap.WRAP
        mAdapter = HistoryAdapter(viewModel.historyList)
        mAdapter.setOnItemClickListener(this)
        binding.recyclerView.apply {
            adapter = mAdapter
            layoutManager = mLayoutManager
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun queryData() {
        viewModel.historyList.clear()
        thread {
            for (element in searchHistoryDao.loadAllHistory()) {
                viewModel.historyList.add(element.keyWord)
            }
            if (viewModel.historyList.isEmpty())
                binding.historyLayout.visibility = View.GONE
            else
                binding.historyLayout.visibility = View.VISIBLE
            requireActivity().runOnUiThread {
                mAdapter.notifyDataSetChanged()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initClearHistory() {
        binding.clearAll.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext()).apply {
                setTitle(R.string.clearAllTitle)
                setNegativeButton(android.R.string.cancel, null)
                setPositiveButton(android.R.string.ok) { _, _ ->
                    thread {
                        searchHistoryDao.deleteAll()
                    }
                    viewModel.historyList.clear()
                    mAdapter.notifyDataSetChanged()
                    binding.historyLayout.visibility = View.GONE
                }
                show()
            }
        }
    }

    private fun initButton() {
        binding.back.setOnClickListener {
            requireActivity().finish()
        }
        binding.search.setOnClickListener {
            search()
        }
    }

    private fun initEdit() {
        binding.editText.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, keyEvent ->
            if ((actionId == EditorInfo.IME_ACTION_UNSPECIFIED || actionId == EditorInfo.IME_ACTION_SEARCH) && keyEvent != null) {
                search()
                return@OnEditorActionListener false
            }
            false
        })
    }

    private fun search() {
        if (binding.editText.text.toString() == "") {
            Toast.makeText(activity, "请输入关键词", Toast.LENGTH_SHORT).show()
            //hideKeyBoard()
        } else {
            hideKeyBoard()
            requireActivity().supportFragmentManager
                .beginTransaction()
                .replace(
                    R.id.searchFragment,
                    SearchResultFragment.newInstance(
                        binding.editText.text.toString(),
                        viewModel.pageType,
                        viewModel.pageParam,
                        viewModel.title
                    ),
                    null
                )
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(null)
                .commit()
            updateHistory(binding.editText.text.toString())
            if (PrefManager.isClearKeyWord)
                binding.editText.text = null
        }
    }

    private fun hideKeyBoard() {
        val im =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        im.hideSoftInputFromWindow(
            requireActivity().currentFocus!!.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
    }

    @SuppressLint("RestrictedApi")
    private fun initEditText() {
        binding.editText.highlightColor = ColorUtils.setAlphaComponent(
            ThemeUtils.getThemeAttrColor(
                requireContext(),
                rikka.preference.simplemenu.R.attr.colorPrimaryDark
            ), 128
        )
        binding.editText.isFocusable = true
        binding.editText.isFocusableInTouchMode = true
        binding.editText.requestFocus()
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.editText, 0)
        binding.editText.imeOptions = EditorInfo.IME_ACTION_SEARCH
        binding.editText.inputType = EditorInfo.TYPE_CLASS_TEXT
        if (viewModel.pageType != "")
            binding.editText.hint = "在 ${viewModel.title} 中搜索"
    }

    override fun onStart() {
        super.onStart()
        initEditText()
    }

    override fun onItemClick(keyword: String) {
        binding.editText.setText(keyword)
        binding.editText.setSelection(keyword.length)
        search()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateHistory(keyword: String) {
        thread {
            if (searchHistoryDao.isExist(keyword)) {
                viewModel.historyList.remove(keyword)
                searchHistoryDao.delete(keyword)
            }
            viewModel.historyList.add(0, keyword)
            searchHistoryDao.insert(SearchHistory(keyword))
            requireActivity().runOnUiThread {
                mAdapter.notifyDataSetChanged()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onItemDeleteClick(keyword: String) {
        thread {
            searchHistoryDao.delete(keyword)
        }
        viewModel.historyList.remove(keyword)
        mAdapter.notifyDataSetChanged()
    }

}