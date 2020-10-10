package com.moonm.chatlib

import android.app.Notification
import android.app.PendingIntent.CanceledException
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
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
import kotlin.collections.HashSet

public object KakaoListener: NotificationListenerService() {
    private var listeners = HashSet<OnChatInListener>()
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

    fun addListener(listener: OnChatInListener) {
        listeners.add(listener)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        if (sbn.packageName == "com.kakao.talk") {
            val wearableExtender = Notification.WearableExtender(sbn.notification)
            for (act in wearableExtender.actions) {
                if (act.remoteInputs!=null && act.remoteInputs.isNotEmpty()) {
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

                    if (listeners.isNotEmpty()) {
                        listeners.forEach {
                            it.onResponse(
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
    }

    fun checkPermission(context: Context,packageName: String):Boolean {
        val permission = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        return !(permission == null || !permission.contains(packageName))
    }

    fun requestPermission() {
        startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
    }

    fun checkAndRequestPermission(context: Context,packageName: String) {
        if (!checkPermission(context,packageName)) requestPermission()
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