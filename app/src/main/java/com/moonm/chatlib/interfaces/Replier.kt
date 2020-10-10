package com.moonm.chatlib.interfaces

import android.app.Notification

interface Replier {
    fun send(msg:String)
    fun send(msg:String,session:Notification.Action)
    var session:Notification.Action
}