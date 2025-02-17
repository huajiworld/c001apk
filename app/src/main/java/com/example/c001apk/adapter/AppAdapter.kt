package com.example.c001apk.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.absinthe.libraries.utils.extensions.dp
import com.example.c001apk.BR
import com.example.c001apk.R
import com.example.c001apk.databinding.ItemCollectionListItemBinding
import com.example.c001apk.databinding.ItemFeedReplyBinding
import com.example.c001apk.databinding.ItemHomeFeedBinding
import com.example.c001apk.databinding.ItemHomeFeedRefreshCardBinding
import com.example.c001apk.databinding.ItemHomeIconLinkGridCardBinding
import com.example.c001apk.databinding.ItemHomeIconMiniScrollCardBinding
import com.example.c001apk.databinding.ItemHomeImageCarouselCardBinding
import com.example.c001apk.databinding.ItemHomeImageSquareScrollCardBinding
import com.example.c001apk.databinding.ItemHomeImageTextScrollCardBinding
import com.example.c001apk.databinding.ItemRecentHistoryBinding
import com.example.c001apk.databinding.ItemSearchApkBinding
import com.example.c001apk.databinding.ItemSearchTopicBinding
import com.example.c001apk.databinding.ItemSearchUserBinding
import com.example.c001apk.logic.model.HomeFeedResponse
import com.example.c001apk.logic.model.IconLinkGridCardBean
import com.example.c001apk.logic.model.Like
import com.example.c001apk.util.BlackListUtil
import com.example.c001apk.util.DateUtils
import com.example.c001apk.util.ImageUtil
import com.example.c001apk.util.PrefManager
import com.example.c001apk.util.TopicBlackListUtil
import com.example.c001apk.util.Utils.getColorFromAttr
import com.example.c001apk.view.LinearItemDecoration1

class AppAdapter(private val listener: ItemListener) : BaseViewTypeAdapter<ViewDataBinding>() {

    class FeedViewHolder(val binding: ItemHomeFeedBinding, val listener: ItemListener) :
        BaseViewHolder<ViewDataBinding>(binding) {
        var entityType: String = ""
        var id: String = ""
        var uid: String = ""
        var likeData: Like = Like()

        init {
            binding.expand.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    menuInflater.inflate(R.menu.feed_reply_menu, menu).apply {
                        menu.findItem(R.id.copy)?.isVisible = false
                        menu.findItem(R.id.delete)?.isVisible = PrefManager.uid == uid
                        menu.findItem(R.id.show)?.isVisible = false
                        menu.findItem(R.id.report)?.isVisible = PrefManager.isLogin
                    }
                    setOnMenuItemClickListener(
                        PopClickListener(
                            listener,
                            it.context,
                            entityType,
                            id,
                            uid,
                            bindingAdapterPosition
                        )
                    )
                    show()
                }
            }

            binding.like.setOnClickListener {
                listener.onLikeClick(
                    entityType, id,
                    bindingAdapterPosition, likeData
                )
            }
        }

        override fun bind(data: HomeFeedResponse.Data) {
            entityType = data.entityType
            id = data.id
            uid = data.uid

            binding.setVariable(BR.data, data)
            binding.setVariable(BR.listener, listener)
            likeData = Like().also {
                it.apply {
                    data.userAction?.like?.let { like ->
                        isLike.set(like)
                    }
                    likeNum.set(data.likenum)
                }
            }
            binding.setVariable(BR.likeData, likeData)
            val lp = ConstraintLayout.LayoutParams(0, ConstraintLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(if (data.infoHtml.isEmpty()) 10.dp else 5.dp, 0, 0, 0)
            lp.topToBottom = binding.uname.id
            lp.startToEnd = binding.from.id
            lp.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            binding.device.layoutParams = lp
        }
    }

    class ImageCarouselCardViewHolder(
        val binding: ItemHomeImageCarouselCardBinding,
        val listener: ItemListener
    ) :
        BaseViewHolder<ViewDataBinding>(binding) {
        override fun bind(data: HomeFeedResponse.Data) {
            if (!data.entities.isNullOrEmpty()) {
                val imageCarouselCard: MutableList<HomeFeedResponse.Entities> = ArrayList()
                data.entities.forEach {
                    if (!it.url.startsWith("http"))
                        imageCarouselCard.add(it)
                }
                val dataList: MutableList<IconLinkGridCardBean> = ArrayList()
                dataList.add(
                    IconLinkGridCardBean(
                        imageCarouselCard[imageCarouselCard.size - 1].title,
                        imageCarouselCard[imageCarouselCard.size - 1].pic,
                        imageCarouselCard[imageCarouselCard.size - 1].url
                    )
                )
                imageCarouselCard.forEach {
                    dataList.add(IconLinkGridCardBean(it.title, it.pic, it.url))
                }
                dataList.add(
                    IconLinkGridCardBean(
                        imageCarouselCard[0].title,
                        imageCarouselCard[0].pic,
                        imageCarouselCard[0].url
                    )
                )
                var currentPosition = 0
                binding.viewPager.adapter = ImageCarouselCardAdapter(listener).also {
                    it.submitList(dataList)
                }
                binding.viewPager.registerOnPageChangeCallback(object :
                    ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        currentPosition = position
                    }

                    override fun onPageScrollStateChanged(state: Int) {
                        if (state == ViewPager2.SCROLL_STATE_IDLE) {
                            if (currentPosition == 0) {
                                binding.viewPager.setCurrentItem(dataList.size - 2, false)
                            } else if (currentPosition == dataList.size - 1) {
                                binding.viewPager.setCurrentItem(1, false)
                            }
                        }
                    }
                })
                binding.viewPager.setCurrentItem(1, false)
            }
        }
    }


    class IconLinkGridCardViewHolder(
        val binding: ItemHomeIconLinkGridCardBinding,
        val listener: ItemListener
    ) :
        BaseViewHolder<ViewDataBinding>(binding) {
        override fun bind(data: HomeFeedResponse.Data) {
            if (!data.entities.isNullOrEmpty()) {
                val dataList = data.entities.map {
                    IconLinkGridCardBean(it.title, it.pic, it.url)
                }
                val maps: MutableList<List<IconLinkGridCardBean>> = ArrayList()
                val page = data.entities.size / 5
                var index = 0
                repeat(page) {
                    maps.add(dataList.subList(index * 5, (index + 1) * 5))
                    index++
                }
                binding.viewPager.adapter = IconLinkGridCardAdapter(listener).also {
                    it.submitList(maps)
                }
                if (page < 2) binding.indicator.visibility = View.GONE
                else {
                    binding.indicator.visibility = View.VISIBLE
                    binding.indicator.setViewPager(binding.viewPager)
                }
            }

        }
    }

    class ImageTextScrollCardViewHolder(
        val binding: ItemHomeImageTextScrollCardBinding,
        val listener: ItemListener
    ) :
        BaseViewHolder<ViewDataBinding>(binding) {
        override fun bind(data: HomeFeedResponse.Data) {
            if (!data.entities.isNullOrEmpty()) {
                val imageTextScrollCard = ArrayList<HomeFeedResponse.Entities>()
                data.entities.forEach {
                    if (it.entityType == "feed" && !BlackListUtil.checkUid(it.userInfo.uid))
                        imageTextScrollCard.add(it)
                }
                binding.title.text = data.title
                binding.title.setPadding(10.dp, 10.dp, 10.dp, 0)
                binding.recyclerView.apply {
                    adapter = ImageTextScrollCardAdapter(listener).also {
                        it.submitList(imageTextScrollCard)
                    }
                    layoutManager = LinearLayoutManager(itemView.context).also {
                        it.orientation = LinearLayoutManager.HORIZONTAL
                    }
                    if (itemDecorationCount == 0)
                        addItemDecoration(LinearItemDecoration1(10.dp))
                }
            }

        }

    }

    class IconMiniScrollCardViewHolder(
        val binding: ItemHomeIconMiniScrollCardBinding,
        val listener: ItemListener
    ) :
        BaseViewHolder<ViewDataBinding>(binding) {
        override fun bind(data: HomeFeedResponse.Data) {
            if (!data.entities.isNullOrEmpty()) {
                val imageTextScrollCard = ArrayList<HomeFeedResponse.Entities>()
                data.entities.forEach {
                    if ((it.entityType == "topic" || it.entityType == "product")
                        && !TopicBlackListUtil.checkTopic(it.title)
                    )
                        imageTextScrollCard.add(it)
                }
                binding.recyclerView.apply {
                    adapter = IconMiniScrollCardAdapter(listener).also {
                        it.submitList(imageTextScrollCard)
                    }
                    layoutManager = LinearLayoutManager(itemView.context).also {
                        it.orientation = LinearLayoutManager.HORIZONTAL
                    }
                    if (itemDecorationCount == 0)
                        addItemDecoration(LinearItemDecoration1(10.dp))
                }
            }

        }
    }

    class RefreshCardViewHolder(val binding: ItemHomeFeedRefreshCardBinding) :
        BaseViewHolder<ViewDataBinding>(binding) {
        override fun bind(data: HomeFeedResponse.Data) {
            binding.textView.text = data.title
        }
    }

    class ImageSquareScrollCardViewHolder(
        val binding: ItemHomeImageSquareScrollCardBinding,
        val listener: ItemListener
    ) :
        BaseViewHolder<ViewDataBinding>(binding) {
        override fun bind(data: HomeFeedResponse.Data) {
            if (!data.entities.isNullOrEmpty()) {
                val imageTextScrollCard = ArrayList<HomeFeedResponse.Entities>()
                data.entities.forEach {
                    if (it.entityType == "picCategory")
                        imageTextScrollCard.add(it)
                }
                binding.recyclerView.apply {
                    adapter = ImageSquareScrollCardAdapter(listener).also {
                        it.submitList(imageTextScrollCard)
                    }
                    layoutManager = LinearLayoutManager(itemView.context).also {
                        it.orientation = LinearLayoutManager.HORIZONTAL
                    }
                    if (itemDecorationCount == 0)
                        addItemDecoration(LinearItemDecoration1(10.dp))
                }
            }
        }
    }

    class UserViewHolder(val binding: ItemSearchUserBinding, val listener: ItemListener) :
        BaseViewHolder<ViewDataBinding>(binding) {
        @SuppressLint("SetTextI18n")
        override fun bind(data: HomeFeedResponse.Data) {

            binding.setVariable(BR.position, bindingAdapterPosition)
            binding.setVariable(BR.listener, listener)

            if (data.userInfo != null && data.fUserInfo != null) {
                binding.uid = data.userInfo.uid
                binding.uname.text = data.userInfo.username
                binding.follow.text = "${data.userInfo.follow}关注"
                binding.fans.text = "${data.userInfo.fans}粉丝"
                binding.act.text = DateUtils.fromToday(data.userInfo.logintime) + "活跃"
                ImageUtil.showIMG(binding.avatar, data.userInfo.userAvatar)
            } else if (data.userInfo == null && data.fUserInfo != null) {
                binding.uid = data.fUserInfo.uid
                binding.uname.text = data.fUserInfo.username
                binding.follow.text = "${data.fUserInfo.follow}关注"
                binding.fans.text = "${data.fUserInfo.fans}粉丝"
                binding.act.text = DateUtils.fromToday(data.fUserInfo.logintime) + "活跃"
                ImageUtil.showIMG(binding.avatar, data.fUserInfo.userAvatar)
            } else if (data.userInfo != null) {
                binding.uid = data.uid
                binding.uname.text = data.username
                binding.follow.text = "${data.follow}关注"
                binding.fans.text = "${data.fans}粉丝"
                binding.act.text = DateUtils.fromToday(data.logintime) + "活跃"
                binding.isFollow = data.isFollow
                if (data.isFollow == 0) {
                    binding.followBtn.text = "关注"
                    binding.followBtn.setTextColor(itemView.context.getColorFromAttr(rikka.preference.simplemenu.R.attr.colorPrimary))
                } else {
                    binding.followBtn.text = "已关注"
                    binding.followBtn.setTextColor(itemView.context.getColor(android.R.color.darker_gray))
                }
                binding.followBtn.visibility = if (PrefManager.isLogin) View.VISIBLE
                else View.GONE
                ImageUtil.showIMG(binding.avatar, data.userAvatar)
            }

        }
    }

    class TopicProductViewHolder(val binding: ItemSearchTopicBinding, val listener: ItemListener) :
        BaseViewHolder<ViewDataBinding>(binding) {
        override fun bind(data: HomeFeedResponse.Data) {
            if (data.description == "home") {
                binding.parent.setCardBackgroundColor(
                    itemView.context.getColorFromAttr(android.R.attr.windowBackground)
                )
            } else {
                binding.parent.setCardBackgroundColor(
                    itemView.context.getColor(R.color.home_card_background_color)
                )
            }
            binding.commentNum.text = if (data.entityType == "topic")
                "${data.commentnumTxt}讨论"
            else
                "${data.feedCommentNumTxt}讨论"

            binding.setVariable(BR.data, data)
            binding.setVariable(BR.listener, listener)
        }
    }

    class AppViewHolder(val binding: ItemSearchApkBinding, val listener: ItemListener) :
        BaseViewHolder<ViewDataBinding>(binding) {
        override fun bind(data: HomeFeedResponse.Data) {
            binding.setVariable(BR.data, data)
            binding.setVariable(BR.listener, listener)
        }
    }

    class CollectionViewHolder(
        val binding: ItemCollectionListItemBinding,
        val listener: ItemListener
    ) :
        BaseViewHolder<ViewDataBinding>(binding) {
        override fun bind(data: HomeFeedResponse.Data) {
            binding.setVariable(BR.data, data)
            binding.setVariable(BR.listener, listener)
        }
    }

    class RecentHistoryViewHolder(
        val binding: ItemRecentHistoryBinding,
        val listener: ItemListener
    ) :
        BaseViewHolder<ViewDataBinding>(binding) {
        override fun bind(data: HomeFeedResponse.Data) {
            binding.setVariable(BR.data, data)
            binding.setVariable(BR.listener, listener)
            binding.fans.text =
                if (data.targetType == "user")
                    "${data.fansNum}粉丝"
                else
                    "${data.commentNum}讨论"
        }
    }

    class FeedReplyViewHolder(val binding: ItemFeedReplyBinding, val listener: ItemListener) :
        BaseViewHolder<ViewDataBinding>(binding) {
        var entityType: String = ""
        var id: String = ""
        var uid: String = ""
        var likeData: Like = Like()

        init {
            binding.expand.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    menuInflater.inflate(R.menu.feed_reply_menu, menu).apply {
                        menu.findItem(R.id.copy)?.isVisible = false
                        menu.findItem(R.id.delete)?.isVisible = PrefManager.uid == uid
                        menu.findItem(R.id.show)?.isVisible = false
                        menu.findItem(R.id.report)?.isVisible = PrefManager.isLogin
                    }
                    setOnMenuItemClickListener(
                        PopClickListener(
                            listener,
                            it.context,
                            entityType,
                            id,
                            uid,
                            bindingAdapterPosition
                        )
                    )
                    show()
                }
            }
            binding.like.setOnClickListener {
                listener.onLikeClick(
                    entityType,
                    id,
                    bindingAdapterPosition,
                    likeData
                )
            }
        }

        override fun bind(data: HomeFeedResponse.Data) {
            entityType = data.entityType
            id = data.id
            uid = data.uid

            binding.setVariable(BR.data, data)
            binding.setVariable(BR.listener, listener)
            likeData = Like().also {
                it.apply {
                    data.userAction?.like?.let { like ->
                        isLike.set(like)
                    }
                    likeNum.set(data.likenum)
                }
            }
            binding.setVariable(BR.likeData, likeData)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BaseViewHolder<ViewDataBinding> {
        return when (viewType) {

            0 -> {
                ImageCarouselCardViewHolder(
                    ItemHomeImageCarouselCardBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent, false
                    ), listener
                )
            }

            1 -> {
                IconLinkGridCardViewHolder(
                    ItemHomeIconLinkGridCardBinding.inflate(
                        LayoutInflater.from(parent.context), parent,
                        false
                    ), listener
                )
            }

            2 -> {
                FeedViewHolder(
                    ItemHomeFeedBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                    listener
                )
            }

            3 -> {
                ImageTextScrollCardViewHolder(
                    ItemHomeImageTextScrollCardBinding.inflate(
                        LayoutInflater.from(parent.context), parent,
                        false
                    ), listener
                )
            }

            4 -> {
                IconMiniScrollCardViewHolder(
                    ItemHomeIconMiniScrollCardBinding.inflate(
                        LayoutInflater.from(parent.context), parent,
                        false
                    ), listener
                )
            }

            5 -> {
                RefreshCardViewHolder(
                    ItemHomeFeedRefreshCardBinding.inflate(
                        LayoutInflater.from(parent.context), parent,
                        false
                    )
                )
            }

            6 -> {
                UserViewHolder(
                    ItemSearchUserBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    ),
                    listener
                )
            }

            7 -> {
                TopicProductViewHolder(
                    ItemSearchTopicBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    ),
                    listener
                )
            }

            8 -> {
                AppViewHolder(
                    ItemSearchApkBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    ),
                    listener
                )
            }

            10 -> {
                FeedReplyViewHolder(
                    ItemFeedReplyBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    ),
                    listener
                )
            }

            11 -> {
                CollectionViewHolder(
                    ItemCollectionListItemBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    ), listener
                )
            }

            12 -> {
                RecentHistoryViewHolder(
                    ItemRecentHistoryBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    ), listener
                )
            }

            13 -> {
                ImageSquareScrollCardViewHolder(
                    ItemHomeImageSquareScrollCardBinding.inflate(
                        LayoutInflater.from(parent.context), parent,
                        false
                    ), listener
                )
            }

            else -> throw IllegalArgumentException("viewType error: $viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (currentList[position].entityType) {
            "card" -> {
                when (currentList[position].entityTemplate) {
                    "imageCarouselCard_1" -> 0
                    "iconLinkGridCard" -> 1
                    "imageTextScrollCard" -> 3

                    "iconMiniScrollCard" -> 4
                    "iconMiniGridCard" -> 4

                    "refreshCard" -> 5

                    "imageSquareScrollCard" -> 13

                    else -> throw IllegalArgumentException("entityType error: ${currentList[position].entityTemplate}")
                }
            }

            "feed" -> when (currentList[position].feedType) {
                "vote" -> 2//9
                else -> 2
            }

            "contacts" -> 6
            "user" -> 6

            "topic" -> 7
            "product" -> 7

            "apk" -> 8

            "feed_reply" -> 10

            "collection" -> 11

            "recentHistory" -> 12

            else -> throw IllegalArgumentException("entityType error: ${currentList[position].entityType}")
        }
    }

}