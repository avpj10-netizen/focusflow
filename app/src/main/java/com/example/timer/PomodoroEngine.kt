package com.example.timer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.ChronoStudyApplication
import com.example.MainActivity
import com.example.R
import com.example.data.StudyRepository
import com.example.widget.ChronoStudyWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

enum class PomodoroMode(val label: String) {
    WORK("Focus"),
    SHORT_BREAK("Short Break"),
    LONG_BREAK("Long Break")
}

enum class TimerStatus {
    IDLE,
    RUNNING,
    PAUSED
}

enum class AmbientSound(val label: String) {
    NONE("Mute"),
    WHITE_NOISE("Soft Static"),
    RAIN("Gentle Rain"),
    CAFE("Campus Hum")
}

data class PomodoroState(
    val mode: PomodoroMode = PomodoroMode.WORK,
    val status: TimerStatus = TimerStatus.IDLE,
    val remainingSeconds: Int = 25 * 60,
    val totalDurationSeconds: Int = 25 * 60,
    val workDurationMinutes: Int = 25,
    val shortBreakDurationMinutes: Int = 5,
    val longBreakDurationMinutes: Int = 15,
    val activeSubjectId: Long? = null,
    val activeSubjectName: String = "General Focus",
    val activeTaskId: Long? = null,
    val activeTaskTitle: String? = null,
    val completedSessionsCount: Int = 0,
    val ambientSound: AmbientSound = AmbientSound.NONE
)

class PomodoroEngine(
    private val context: Context,
    private val repository: StudyRepository
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var tickerJob: Job? = null
    private var ambientTrack: AudioTrack? = null
    private var isPlayingAmbient = false

    private val _state = MutableStateFlow(PomodoroState())
    val state: StateFlow<PomodoroState> = _state.asStateFlow()

    fun toggle() {
        if (_state.value.status == TimerStatus.RUNNING) {
            pause()
        } else {
            start()
        }
    }

    fun start() {
        if (_state.value.status == TimerStatus.RUNNING) return

        _state.update { it.copy(status = TimerStatus.RUNNING) }
        startTicker()
        startAmbientIfNeeded(_state.value.ambientSound)
        triggerHaptic()
        updateWidget()
    }

    fun pause() {
        if (_state.value.status != TimerStatus.RUNNING) return

        tickerJob?.cancel()
        _state.update { it.copy(status = TimerStatus.PAUSED) }
        stopAmbient()
        triggerHaptic()
        updateWidget()
    }

    fun reset() {
        tickerJob?.cancel()
        stopAmbient()
        val current = _state.value
        val defaultSeconds = when (current.mode) {
            PomodoroMode.WORK -> current.workDurationMinutes * 60
            PomodoroMode.SHORT_BREAK -> current.shortBreakDurationMinutes * 60
            PomodoroMode.LONG_BREAK -> current.longBreakDurationMinutes * 60
        }
        _state.update {
            it.copy(
                status = TimerStatus.IDLE,
                remainingSeconds = defaultSeconds,
                totalDurationSeconds = defaultSeconds
            )
        }
        triggerHaptic()
        updateWidget()
    }

    fun skip() {
        tickerJob?.cancel()
        stopAmbient()
        switchNextMode(completedNaturally = false)
        updateWidget()
    }

    fun setMode(newMode: PomodoroMode) {
        tickerJob?.cancel()
        stopAmbient()
        val current = _state.value
        val seconds = when (newMode) {
            PomodoroMode.WORK -> current.workDurationMinutes * 60
            PomodoroMode.SHORT_BREAK -> current.shortBreakDurationMinutes * 60
            PomodoroMode.LONG_BREAK -> current.longBreakDurationMinutes * 60
        }
        _state.update {
            it.copy(
                mode = newMode,
                status = TimerStatus.IDLE,
                remainingSeconds = seconds,
                totalDurationSeconds = seconds
            )
        }
        updateWidget()
    }

    fun setDurations(workMinutes: Int, shortBreakMinutes: Int, longBreakMinutes: Int) {
        val current = _state.value
        val seconds = when (current.mode) {
            PomodoroMode.WORK -> workMinutes * 60
            PomodoroMode.SHORT_BREAK -> shortBreakMinutes * 60
            PomodoroMode.LONG_BREAK -> longBreakMinutes * 60
        }
        _state.update {
            it.copy(
                workDurationMinutes = workMinutes,
                shortBreakDurationMinutes = shortBreakMinutes,
                longBreakDurationMinutes = longBreakMinutes,
                remainingSeconds = if (it.status == TimerStatus.IDLE) seconds else it.remainingSeconds,
                totalDurationSeconds = if (it.status == TimerStatus.IDLE) seconds else it.totalDurationSeconds
            )
        }
    }

    fun setActiveSubject(subjectId: Long?, name: String) {
        _state.update {
            it.copy(activeSubjectId = subjectId, activeSubjectName = name)
        }
        updateWidget()
    }

    fun setActiveTask(taskId: Long?, title: String?) {
        _state.update {
            it.copy(activeTaskId = taskId, activeTaskTitle = title)
        }
        updateWidget()
    }

    fun setAmbientSound(sound: AmbientSound) {
        _state.update { it.copy(ambientSound = sound) }
        if (_state.value.status == TimerStatus.RUNNING) {
            startAmbientIfNeeded(sound)
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive && _state.value.remainingSeconds > 0) {
                delay(1000)
                _state.update { current ->
                    val nextSec = current.remainingSeconds - 1
                    current.copy(remainingSeconds = nextSec)
                }
            }
            if (_state.value.remainingSeconds <= 0) {
                onTimerFinished()
            }
        }
    }

    private fun onTimerFinished() {
        tickerJob?.cancel()
        stopAmbient()
        val finishedState = _state.value

        // Log session in repository
        scope.launch(Dispatchers.IO) {
            val minutes = when (finishedState.mode) {
                PomodoroMode.WORK -> finishedState.workDurationMinutes
                PomodoroMode.SHORT_BREAK -> finishedState.shortBreakDurationMinutes
                PomodoroMode.LONG_BREAK -> finishedState.longBreakDurationMinutes
            }
            repository.recordFocusSession(
                subjectId = finishedState.activeSubjectId,
                taskId = finishedState.activeTaskId,
                durationMinutes = minutes,
                sessionType = finishedState.mode.name
            )
        }

        // Notify user with alarm notification & sound
        fireCompletionNotification(finishedState.mode)
        triggerVibrateNotification()

        // Transition to next mode
        switchNextMode(completedNaturally = true)
        updateWidget()
    }

    private fun switchNextMode(completedNaturally: Boolean) {
        val current = _state.value
        val newCount = if (completedNaturally && current.mode == PomodoroMode.WORK) {
            current.completedSessionsCount + 1
        } else current.completedSessionsCount

        val nextMode = when (current.mode) {
            PomodoroMode.WORK -> {
                if (newCount > 0 && newCount % 4 == 0) PomodoroMode.LONG_BREAK
                else PomodoroMode.SHORT_BREAK
            }
            PomodoroMode.SHORT_BREAK, PomodoroMode.LONG_BREAK -> PomodoroMode.WORK
        }

        val nextSeconds = when (nextMode) {
            PomodoroMode.WORK -> current.workDurationMinutes * 60
            PomodoroMode.SHORT_BREAK -> current.shortBreakDurationMinutes * 60
            PomodoroMode.LONG_BREAK -> current.longBreakDurationMinutes * 60
        }

        _state.update {
            it.copy(
                mode = nextMode,
                status = TimerStatus.IDLE,
                remainingSeconds = nextSeconds,
                totalDurationSeconds = nextSeconds,
                completedSessionsCount = newCount
            )
        }
    }

    private fun fireCompletionNotification(completedMode: PomodoroMode) {
        val title = when (completedMode) {
            PomodoroMode.WORK -> "Focus Session Complete! 🎉"
            PomodoroMode.SHORT_BREAK -> "Break Over! Time to refocus 📚"
            PomodoroMode.LONG_BREAK -> "Rest Complete! Ready for another round? 🚀"
        }
        val message = when (completedMode) {
            PomodoroMode.WORK -> "Great job on ${_state.value.activeSubjectName}. Take a well-deserved break."
            PomodoroMode.SHORT_BREAK, PomodoroMode.LONG_BREAK -> "Your break is finished. Tap to begin your next focus block."
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, ChronoStudyApplication.CHANNEL_POMODORO)
            .setSmallIcon(R.drawable.chronostudy_launcher_icon_1794023474724)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 400, 200, 400))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(2001, notification)
        } catch (_: SecurityException) {
            // Permission might be pending
        }
    }

    private fun triggerHaptic() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        } catch (_: Exception) {}
    }

    private fun triggerVibrateNotification() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 300, 150, 300), -1)
            }
        } catch (_: Exception) {}
    }

    private fun startAmbientIfNeeded(sound: AmbientSound) {
        stopAmbient()
        if (sound == AmbientSound.NONE) return

        scope.launch(Dispatchers.Default) {
            try {
                isPlayingAmbient = true
                val sampleRate = 22050
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = minBufferSize.coerceAtLeast(sampleRate)
                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                ambientTrack = audioTrack
                audioTrack.play()

                val buffer = ShortArray(bufferSize / 2)
                var phase = 0.0

                while (isPlayingAmbient && isActive) {
                    when (sound) {
                        AmbientSound.WHITE_NOISE -> {
                            // Gentle filtered white noise
                            for (i in buffer.indices) {
                                buffer[i] = ((Random.nextFloat() * 2f - 1f) * 1500).toInt().toShort()
                            }
                        }
                        AmbientSound.RAIN -> {
                            // Rain patter with gentle low-pass pulses
                            for (i in buffer.indices) {
                                val drop = if (Random.nextFloat() < 0.04f) 3000 else 0
                                val background = ((Random.nextFloat() * 2f - 1f) * 1200).toInt()
                                buffer[i] = (drop + background).coerceIn(-32768, 32767).toShort()
                            }
                        }
                        AmbientSound.CAFE -> {
                            // Warm ambient drone with binaural hum
                            val freq = 136.1 // Ohm tone (relaxing study pitch)
                            for (i in buffer.indices) {
                                val s = (sin(phase) * 2000).toInt()
                                buffer[i] = s.toShort()
                                phase += 2 * Math.PI * freq / sampleRate
                                if (phase > 2 * Math.PI) phase -= 2 * Math.PI
                            }
                        }
                        AmbientSound.NONE -> break
                    }
                    audioTrack.write(buffer, 0, buffer.size)
                }
            } catch (_: Exception) {
                // Ignore audio track exception
            }
        }
    }

    private fun stopAmbient() {
        isPlayingAmbient = false
        try {
            ambientTrack?.stop()
            ambientTrack?.release()
        } catch (_: Exception) {}
        ambientTrack = null
    }

    private fun updateWidget() {
        ChronoStudyWidgetProvider.updateAllWidgets(context)
    }
}
