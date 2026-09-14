package com.airplayr.app.core.playback

import kotlin.math.pow

const val MIN_SPEED = 0.25
const val MAX_SPEED = 4.0
const val MIN_PITCH_SEMITONES = -12
const val MAX_PITCH_SEMITONES = 12
const val MAX_CROSSFADE_SECONDS = 12

fun pitchRatio(semitones: Int): Float = 2.0.pow(semitones.coerceIn(MIN_PITCH_SEMITONES, MAX_PITCH_SEMITONES) / 12.0).toFloat()

fun linkedPitchSemitones(speed: Double): Int = kotlin.math.round(12.0 * kotlin.math.log2(speed.coerceIn(MIN_SPEED, MAX_SPEED))).toInt().coerceIn(MIN_PITCH_SEMITONES, MAX_PITCH_SEMITONES)
