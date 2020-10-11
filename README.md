kakao-chatbot-base
=======
[![](https://jitpack.io/v/mooner1022/kakao-chatbot-base.svg)](https://jitpack.io/#mooner1022/kakao-chatbot-base)

kakao-chatbot-base is a simple library for developing KakaoTalk chatbot on Android

Installation
------------
```gradle
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}

dependencies {
    implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
}
```

Usage
------------
### Requesting permissions
Kotlin
```kotlin
val listener = KakaoListener() //define KakaoListener

listener.checkPermission(context) //Checks if app has permission

listener.requestPermission(context) //Requests user to allow permission

listener.checkAndRequestPermission(context) //Check permission and request
```

Java
```java
KakaoListener listener = new KakaoListener();

listener.checkPermission(context);

listener.requestPermission(context);

listener.checkAndRequestPermission(context);
```

### Register listener
values
```
room - name of the room message was sent
message - content of the message
sender - name of the sender who sent the message
isGroupChat - true if room is group chat, if not false
imageHash - hashcode of sender's profile image
replier - interface for sending messages
```
Kotlin
```kotlin
listener.registerListener("name",object : OnChatInListener { //register listener as 'name'
    override fun onResponse(room: String, message: String, sender: String, isGroupChat: Boolean, imageHash: Long, replier: Replier) {
        // called on message in
        replier.send("replier.send() called") //send message
    }
})
```

Java
```java
listener.registerListener("name", new OnChatInListener() {
    @Override
    public void onResponse(@NotNull String room, @NotNull String message, @NotNull String sender, boolean isGroupChat, long imageHash, @NotNull Replier replier) {
        replier.send("replier.send() called");
    }
});
```

### Unregister listener
Kotlin
```kotlin
listener.unregisterListener("name") //unregister listener 'name'
```

Java
```java
listener.unregisterListener("name");
```

### Others
```kotlin
listener.getListener("name") //return listener 'name'
listener.getListeners() //return all listener registered
```
