package com.example.ui.components

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import java.io.File
import java.io.FileOutputStream

/**
 * Helper audio ringan untuk Voice-Note Forum Diskusi Lembaga.
 * Durasi dibatasi maksimal 30 detik & bitrate rendah (16 kbps mono AAC)
 * untuk menjaga kuota Firebase Firestore (< ~100 KB per pesan suara).
 */
class ForumVoiceHelper(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var recordingStartMs: Long = 0L

    private var player: MediaPlayer? = null
    private var currentPlayingId: String? = null

    fun startRecording(): Result<Unit> = runCatching {
        stopRecordingCleanupOnly()
        val file = File(context.cacheDir, "forum_vn_${System.currentTimeMillis()}.m4a")
        outputFile = file

        @Suppress("DEPRECATION")
        val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
        mr.setAudioSource(MediaRecorder.AudioSource.MIC)
        mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mr.setAudioChannels(1)
        mr.setAudioSamplingRate(16000)
        mr.setAudioEncodingBitRate(16000)
        mr.setMaxDuration(30_000)
        mr.setOutputFile(file.absolutePath)
        mr.prepare()
        mr.start()
        recorder = mr
        recordingStartMs = System.currentTimeMillis()
    }

    /**
     * Menghentikan rekaman dan mengembalikan Pair(base64, durationSeconds).
     */
    fun stopRecording(): Result<Pair<String, Int>> = runCatching {
        val mr = recorder ?: throw IllegalStateException("Perekaman tidak aktif")
        val file = outputFile ?: throw IllegalStateException("File rekaman tidak ditemukan")
        val elapsedSec = (((System.currentTimeMillis() - recordingStartMs) + 500L) / 1000L)
            .toInt()
            .coerceIn(1, 30)

        runCatching { mr.stop() }
        runCatching { mr.release() }
        recorder = null

        val bytes = if (file.exists()) file.readBytes() else ByteArray(0)
        runCatching { file.delete() }
        outputFile = null

        if (bytes.isEmpty()) {
            throw IllegalStateException("Audio voice-note kosong, coba rekam minimal 1 detik.")
        }
        val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        if (b64.length > 135_000) {
            throw IllegalStateException("Ukuran voice-note terlalu besar untuk kuota Firebase. Maksimal 30 detik.")
        }
        b64 to elapsedSec
    }

    fun cancelRecording() {
        stopRecordingCleanupOnly()
    }

    private fun stopRecordingCleanupOnly() {
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        outputFile?.let { runCatching { it.delete() } }
        outputFile = null
    }

    fun playOrStopVoiceNote(
        messageId: String,
        base64Audio: String,
        onStateChanged: (playingId: String?) -> Unit,
        onError: (String) -> Unit
    ) {
        if (currentPlayingId == messageId) {
            stopPlayback()
            onStateChanged(null)
            return
        }
        stopPlayback()

        if (base64Audio.isBlank()) {
            onError("Data audio voice-note tidak tersedia pada pesan contoh ini.")
            onStateChanged(null)
            return
        }

        runCatching {
            val decoded = Base64.decode(base64Audio, Base64.DEFAULT)
            val tempFile = File(context.cacheDir, "play_vn_${messageId}.m4a")
            FileOutputStream(tempFile).use { it.write(decoded) }

            val mp = MediaPlayer()
            mp.setDataSource(tempFile.absolutePath)
            mp.setOnCompletionListener {
                stopPlayback()
                onStateChanged(null)
                runCatching { tempFile.delete() }
            }
            mp.setOnErrorListener { _, _, _ ->
                stopPlayback()
                onStateChanged(null)
                onError("Gagal memutar audio voice-note.")
                true
            }
            mp.prepare()
            mp.start()
            player = mp
            currentPlayingId = messageId
            onStateChanged(messageId)
        }.onFailure { err ->
            stopPlayback()
            onStateChanged(null)
            onError(err.localizedMessage ?: "Audio voice-note tidak dapat diputar.")
        }
    }

    fun stopPlayback() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        currentPlayingId = null
    }

    fun releaseAll() {
        stopRecordingCleanupOnly()
        stopPlayback()
    }
}
