package com.moonm.chatlib.interfaces

interface OnChatInListener {
    fun onResponse(room:String,message:String,sender:String,isGroupChat:Boolean,imageHash:Long,replier:Replier)
}