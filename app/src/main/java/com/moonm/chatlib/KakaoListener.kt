package com.moonm.chatlib

import android.app.Notification
import android.app.PendingIntent.CanceledException
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.SpannableString
import android.util.Base64
import com.moonm.chatlib.interfaces.OnChatInListener
import com.moonm.chatlib.interfaces.Replier
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.collections.HashMap

private object ProcessHandler {
    private val listeners = HashMap<String,OnChatInListener>()
    fun registerListener(code:String,listener: OnChatInListener) {
        if (!listeners.containsKey(code)) {
            listeners[code] = listener
        }
    }
    fun unregisterListener(code:String) {
        listeners.remove(code)
    }

    fun getListeners():HashMap<String,OnChatInListener> {
        return listeners
    }
    fun getListener(code:String):OnChatInListener? {
        return listeners[code]
    }
}

class KakaoListener: NotificationListenerService() {
    private lateinit var _session:Notification.Action
    private val replier:Replier = object : Replier {
        override fun send(msg: String) {
            this@KakaoListener.send(msg, session)
        }

        override fun send(msg: String, session: Notification.Action) {
            this@KakaoListener.send(msg, session)
        }

        override var session: Notification.Action
            get() = _session
            set(value) { _session = value}
    }
    fun registerListener(code:String,listener: OnChatInListener) {
        ProcessHandler.registerListener(code,listener)
    }

    fun unregisterListener(code:String) {
        ProcessHandler.unregisterListener(code)
    }

    fun getListeners() : HashMap<String,OnChatInListener> {
        return ProcessHandler.getListeners()
    }

    fun getListener(code:String):OnChatInListener? {
        return ProcessHandler.getListeners()[code]
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        if (sbn.packageName == "com.kakao.talk") {
            val wearableExtender = Notification.WearableExtender(sbn.notification)
            for (act in wearableExtender.actions) {
                if (act.remoteInputs!=null && act.remoteInputs.isNotEmpty()) {
                    _session = act
                    val processHandler = ProcessHandler
                    val notification = sbn.notification
                    val message = notification.extras["android.text"].toString()
                    val sender = notification.extras.getString("android.title").toString()
                    val room = act.title.toString().replaceFirst("답장 (", "").replace(")", "")
                    val imageHash = encodeIcon(
                        notification.getLargeIcon().loadDrawable(
                            applicationContext
                        )
                    )
                    val isGroupChat = notification.extras["android.text"] is SpannableString
                    processHandler.getListeners().forEach {(_, listener) ->
                        listener.onResponse(
                            room = room,
                            sender = sender,
                            message = message,
                            imageHash = imageHash,
                            isGroupChat = isGroupChat,
                            replier = replier
                        )
                    }
                }
            }
        }
    }

    fun checkPermission(context: Context):Boolean {
        val permission = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        return !(permission == null || !permission.contains(context.packageName))
    }

    fun requestPermission(context: Context) {
        context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").addFlags(FLAG_ACTIVITY_NEW_TASK))
    }

    fun checkAndRequestPermission(context: Context) {
        if (!checkPermission(context)) requestPermission(context)
    }

    private fun send(message: String, session: Notification.Action) {
        val sendIntent = Intent()
        val msg = Bundle()
        for (input in session.remoteInputs) msg.putCharSequence(
            input.resultKey,
            message
        )
        RemoteInput.addResultsToIntent(session.remoteInputs, sendIntent, msg)
        try {
            session.actionIntent.send(applicationContext, 0, sendIntent)
            //Log.i("send() complete", msg)
        } catch (e: CanceledException) {
            e.printStackTrace()
        }
    }

    private fun encodeIcon(icon: Drawable?): Long {
        if (icon != null) {
            val bitDw = icon as BitmapDrawable
            val bitmap = bitDw.bitmap
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            var bitmapByte = stream.toByteArray()
            bitmapByte = Base64.encode(bitmapByte, Base64.DEFAULT)
            return Arrays.hashCode(bitmapByte).toLong()
        }
        return 0
    }
}