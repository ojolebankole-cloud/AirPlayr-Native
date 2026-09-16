package com.airplayr.app.core.video

data class VideoPlayerState(
    val id:String?=null,
    val positionMs:Long=0,
    val durationMs:Long=0,
    val isPlaying:Boolean=false,
    val speed:Float=1f,
    val repeatA:Long?=null,
    val repeatB:Long?=null,
    val completed:Boolean=false,
)
