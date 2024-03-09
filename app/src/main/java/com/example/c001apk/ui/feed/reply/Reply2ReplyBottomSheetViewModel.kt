package com.example.c001apk.ui.feed.reply

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.c001apk.adapter.Event
import com.example.c001apk.adapter.FooterAdapter
import com.example.c001apk.constant.Constants.LOADING_FAILED
import com.example.c001apk.logic.model.Like
import com.example.c001apk.logic.model.TotalReplyResponse
import com.example.c001apk.logic.network.Repository
import com.example.c001apk.logic.network.Repository.getReply2Reply
import com.example.c001apk.util.BlackListUtil
import com.example.c001apk.util.PrefManager
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.net.URLDecoder

class Reply2ReplyBottomSheetViewModel : ViewModel() {

    var uname: String? = null
    var ruid: String? = null
    var rid: String? = null
    var position: Int? = null
    var fuid: String? = null
    var listSize: Int = -1
    var listType: String = "lastupdate_desc"
    var page = 1
    var lastItem: String? = null
    var isInit: Boolean = true
    var isRefreshing: Boolean = true
    var isLoadMore: Boolean = false
    var isEnd: Boolean = false
    var lastVisibleItemPosition: Int = 0
    var itemCount = 1
    var uid: String? = null
    var avatar: String? = null
    var device: String? = null
    var replyCount: String? = null
    var dateLine: Long? = null
    var feedType: String? = null
    var errorMessage: String? = null
    var id: String? = null

    val changeState = MutableLiveData<Pair<FooterAdapter.LoadState, String?>>()
    val totalReplyData = MutableLiveData<List<TotalReplyResponse.Data>>()
    var oriReply: ArrayList<TotalReplyResponse.Data> = ArrayList()

    fun fetchReplyTotal() {
        viewModelScope.launch {
            getReply2Reply(id.toString(), page, lastItem)
                .onStart {
                    if (isLoadMore)
                        changeState.postValue(Pair(FooterAdapter.LoadState.LOADING, null))
                }
                .collect { result ->
                    val replyTotalList = totalReplyData.value?.toMutableList() ?: ArrayList()
                    val reply = result.getOrNull()
                    if (reply?.message != null) {
                        changeState.postValue(
                            Pair(
                                FooterAdapter.LoadState.LOADING_ERROR,
                                reply.message
                            )
                        )
                        return@collect
                    } else if (!reply?.data.isNullOrEmpty()) {
                        lastItem = reply?.data?.last()?.id
                        if (!isLoadMore) {
                            replyTotalList.clear()
                            replyTotalList.addAll(oriReply)
                        }
                        listSize = replyTotalList.size
                        for (element in reply?.data!!)
                            if (element.entityType == "feed_reply")
                                if (!BlackListUtil.checkUid(element.uid))
                                    replyTotalList.add(element)
                        changeState.postValue(Pair(FooterAdapter.LoadState.LOADING_COMPLETE, null))
                    } else if (reply?.data?.isEmpty() == true) {
                        if (replyTotalList.isEmpty())
                            replyTotalList.addAll(oriReply)
                        changeState.postValue(Pair(FooterAdapter.LoadState.LOADING_END, null))
                        isEnd = true
                        result.exceptionOrNull()?.printStackTrace()
                    } else {
                        if (replyTotalList.isEmpty())
                            replyTotalList.addAll(oriReply)
                        changeState.postValue(
                            Pair(
                                FooterAdapter.LoadState.LOADING_ERROR, LOADING_FAILED
                            )
                        )
                        isEnd = true
                        result.exceptionOrNull()?.printStackTrace()
                    }
                    totalReplyData.postValue(replyTotalList)
                }
        }

    }

    val closeSheet = MutableLiveData<Event<Boolean>>()
    var replyData = HashMap<String, String>()
    fun onPostReply() {
        viewModelScope.launch {
            Repository.postReply(replyData, rid.toString(), "reply")
                .collect { result ->
                    val replyTotalList = totalReplyData.value?.toMutableList() ?: ArrayList()
                    val response = result.getOrNull()
                    response?.let {
                        if (response.data != null) {
                            if (response.data.id != null) {
                                toastText.postValue(Event("回复成功"))
                                closeSheet.postValue(Event(true))
                                // generate
                                replyTotalList.add(
                                    position!! + 1,
                                    TotalReplyResponse.Data(
                                        null,
                                        "feed_reply",
                                        (12345678..87654321).random().toString(),
                                        ruid.toString(),
                                        PrefManager.uid,
                                        id.toString(),
                                        URLDecoder.decode(PrefManager.username, "UTF-8"),
                                        uname.toString(),
                                        replyData["message"].toString(),
                                        "",
                                        null,
                                        System.currentTimeMillis() / 1000,
                                        "0",
                                        "0",
                                        PrefManager.userAvatar,
                                        ArrayList(),
                                        0,
                                        TotalReplyResponse.UserAction(0)
                                    )
                                )
                                totalReplyData.postValue(replyTotalList)
                            }
                        } else {
                            response.message?.let {
                                toastText.postValue(Event(it))
                            }
                            if (response.messageStatus == "err_request_captcha") {
                                onGetValidateCaptcha()
                            }
                        }
                    }
                }
        }
    }

    val createDialog = MutableLiveData<Event<Bitmap>>()
    private fun onGetValidateCaptcha() {
        viewModelScope.launch {
            Repository.getValidateCaptcha("/v6/account/captchaImage?${System.currentTimeMillis() / 1000}&w=270=&h=113")
                .collect { result ->
                    val response = result.getOrNull()
                    response?.let {
                        val responseBody = response.body()
                        val bitmap = BitmapFactory.decodeStream(responseBody!!.byteStream())
                        createDialog.postValue(Event(bitmap))
                    }
                }
        }
    }

    val toastText = MutableLiveData<Event<String>>()
    fun postDeleteFeedReply(url: String, id: String, position: Int) {
        viewModelScope.launch {
            Repository.postDelete(url, id)
                .collect { result ->
                    val response = result.getOrNull()
                    if (response != null) {
                        if (response.data == "删除成功") {
                            toastText.postValue(Event("删除成功"))
                            val replyList =
                                totalReplyData.value?.toMutableList() ?: ArrayList()
                            replyList.removeAt(position)
                            totalReplyData.postValue(replyList)
                        } else if (!response.message.isNullOrEmpty()) {
                            response.message.let {
                                toastText.postValue(Event(it))
                            }
                        }
                    } else {
                        result.exceptionOrNull()?.printStackTrace()
                    }
                }
        }
    }

    fun onPostLikeReply(id: String, position: Int, likeData: Like) {
        val likeType = if (likeData.isLike.get() == 1) "unLikeReply"
        else "likeReply"
        val likeUrl = "/v6/feed/$likeType"
        viewModelScope.launch {
            Repository.postLikeReply(likeUrl, id)
                .catch { err ->
                    err.message?.let {
                        toastText.postValue(Event(it))
                    }
                }
                .collect { result ->
                    val response = result.getOrNull()
                    if (response != null) {
                        if (response.data != null) {
                            val count = response.data
                            val isLike = if (likeData.isLike.get() == 1) 0 else 1
                            likeData.likeNum.set(count)
                            likeData.isLike.set(isLike)
                            val currentList = totalReplyData.value!!.toMutableList()
                            currentList[position].likenum = count
                            currentList[position].userAction?.like = isLike
                            totalReplyData.postValue(currentList)
                        } else {
                            response.message?.let {
                                toastText.postValue(Event(it))
                            }
                        }
                    } else {
                        result.exceptionOrNull()?.printStackTrace()
                    }
                }
        }
    }

    lateinit var requestValidateData: HashMap<String, String?>
    fun onPostRequestValidate() {
        viewModelScope.launch {
            Repository.postRequestValidate(requestValidateData)
                .collect { result ->
                    val response = result.getOrNull()
                    response?.let {
                        if (response.data != null) {
                            response.data.let {
                                toastText.postValue(Event(it))
                            }
                            if (response.data == "验证通过") {
                                onPostReply()
                            }
                        } else if (response.message != null) {
                            response.message.let {
                                toastText.postValue(Event(it))
                            }
                            if (response.message == "请输入正确的图形验证码") {
                                onGetValidateCaptcha()
                            }
                        }
                    }
                }
        }
    }

}