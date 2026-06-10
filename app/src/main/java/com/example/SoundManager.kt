package com.example

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

enum class SoundTheme(val display: String) {
    SILENT("Kimya (Mute)"),
    WOODEN("Kawaida (Classic Wood)"),
    ARCADE("Zamani (Arcade Retro)"),
    DIGITAL("Kisasa (Modern UI)")
}

class SoundManager {
    private val sampleRate = 44100
    private val format = AudioFormat.ENCODING_PCM_16BIT

    fun playMoveSound(theme: SoundTheme) {
        if (theme == SoundTheme.SILENT) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (theme) {
                    SoundTheme.WOODEN -> playTone(400.0, 30, 0.5)
                    SoundTheme.ARCADE -> playSweep(800.0, 1200.0, 100, 0.4)
                    SoundTheme.DIGITAL -> playTone(1200.0, 20, 0.3)
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e("SoundManager", "Error playing sound", e)
            }
        }
    }

    fun playCaptureSound(theme: SoundTheme) {
        if (theme == SoundTheme.SILENT) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (theme) {
                    SoundTheme.WOODEN -> {
                        playTone(300.0, 40, 0.5)
                        Thread.sleep(10)
                        playTone(250.0, 60, 0.5)
                    }
                    SoundTheme.ARCADE -> playSweep(1200.0, 400.0, 150, 0.4)
                    SoundTheme.DIGITAL -> {
                        playTone(1500.0, 30, 0.3)
                        Thread.sleep(10)
                        playTone(1800.0, 40, 0.3)
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e("SoundManager", "Error playing sound", e)
            }
        }
    }

    fun playWinSound(theme: SoundTheme) {
        if (theme == SoundTheme.SILENT) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (theme) {
                    SoundTheme.WOODEN -> {
                        playTone(440.0, 150, 0.5)
                        Thread.sleep(50)
                        playTone(554.0, 150, 0.5)
                        Thread.sleep(50)
                        playTone(659.0, 300, 0.5)
                    }
                    SoundTheme.ARCADE -> {
                        playSweep(400.0, 800.0, 100, 0.4)
                        Thread.sleep(50)
                        playSweep(600.0, 1200.0, 200, 0.4)
                    }
                    SoundTheme.DIGITAL -> {
                        playTone(1000.0, 100, 0.3)
                        Thread.sleep(50)
                        playTone(1500.0, 100, 0.3)
                        Thread.sleep(50)
                        playTone(2000.0, 200, 0.3)
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e("SoundManager", "Error playing sound", e)
            }
        }
    }

    fun playLossSound(theme: SoundTheme) {
        if (theme == SoundTheme.SILENT) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (theme) {
                    SoundTheme.WOODEN -> {
                        playTone(300.0, 200, 0.5)
                        Thread.sleep(50)
                        playTone(250.0, 300, 0.5)
                    }
                    SoundTheme.ARCADE -> {
                        playSweep(400.0, 200.0, 300, 0.4)
                    }
                    SoundTheme.DIGITAL -> {
                        playSweep(800.0, 400.0, 300, 0.3)
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e("SoundManager", "Error playing sound", e)
            }
        }
    }

    private fun playTone(freqOfTone: Double, durationMs: Int, volume: Double) {
        val numSamples = (durationMs * sampleRate) / 1000
        if(numSamples <= 0) return
        val sample = DoubleArray(numSamples)
        val generatedSnd = ByteArray(2 * numSamples)

        for (i in 0 until numSamples) {
            val fadeSamples = (0.1 * numSamples).toInt()
            val env = when {
                fadeSamples > 0 && i < fadeSamples -> i.toDouble() / fadeSamples
                fadeSamples > 0 && i > numSamples - fadeSamples -> (numSamples - i).toDouble() / fadeSamples
                else -> 1.0
            }
            sample[i] = sin(2 * Math.PI * i / (sampleRate / freqOfTone)) * env
        }

        var idx = 0
        for (dVal in sample) {
            val v = (dVal * 32767 * volume).toInt()
            val finalVal = (v.toShort()).toInt()
            generatedSnd[idx++] = (finalVal and 0x00ff).toByte()
            generatedSnd[idx++] = ((finalVal and 0xff00) ushr 8).toByte()
        }

        playBytes(generatedSnd, durationMs)
    }

    private fun playSweep(startFreq: Double, endFreq: Double, durationMs: Int, volume: Double) {
        val numSamples = (durationMs * sampleRate) / 1000
        if(numSamples <= 0) return
        val sample = DoubleArray(numSamples)
        val generatedSnd = ByteArray(2 * numSamples)

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val freq = startFreq + (endFreq - startFreq) * progress
            val fadeSamples = (0.1 * numSamples).toInt()
            val env = when {
                fadeSamples > 0 && i < fadeSamples -> i.toDouble() / fadeSamples
                fadeSamples > 0 && i > numSamples - fadeSamples -> (numSamples - i).toDouble() / fadeSamples
                else -> 1.0
            }
            sample[i] = sin(2 * Math.PI * i / (sampleRate / freq)) * env
        }

        var idx = 0
        for (dVal in sample) {
            val v = (dVal * 32767 * volume).toInt()
            val finalVal = (v.toShort()).toInt()
            generatedSnd[idx++] = (finalVal and 0x00ff).toByte()
            generatedSnd[idx++] = ((finalVal and 0xff00) ushr 8).toByte()
        }

        playBytes(generatedSnd, durationMs)
    }

    private fun playBytes(generatedSnd: ByteArray, durationMs: Int) {
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(format)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(generatedSnd.size)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        
        audioTrack.write(generatedSnd, 0, generatedSnd.size)
        audioTrack.play()
        
        Thread.sleep((durationMs + 50).toLong())
        audioTrack.release()
    }
}
