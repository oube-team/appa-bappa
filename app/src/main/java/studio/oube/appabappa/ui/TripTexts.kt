package studio.oube.appabappa.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val SECONDS_PER_MINUTE = 60
private const val MINUTES_PER_HOUR = 60
private const val MILLIS_PER_SECOND = 1_000

// 도착 뒤 화면의 출발, 도착 시각이다(예: 오후 2:05).
internal fun clockText(millis: Long, zone: TimeZone = TimeZone.getDefault()): String =
    SimpleDateFormat("a h:mm", Locale.KOREA).apply { timeZone = zone }.format(Date(millis))

// 바빴던 시간이다. 1분이 안 되면 초만, 1시간 이상이면 시간과 분만 보여 준다(예: 45초, 3분 30초, 1시간 5분).
internal fun durationText(millis: Long): String {
    val seconds = (millis / MILLIS_PER_SECOND).toInt()
    val minutes = seconds / SECONDS_PER_MINUTE
    val hours = minutes / MINUTES_PER_HOUR
    return when {
        hours > 0 -> units(hours, "시간", minutes % MINUTES_PER_HOUR, "분")
        minutes > 0 -> units(minutes, "분", seconds % SECONDS_PER_MINUTE, "초")
        else -> "${seconds}초"
    }
}

// 큰 단위는 늘 보여 주고, 작은 단위는 0이면 생략한다(1시간, 3분).
private fun units(major: Int, majorUnit: String, minor: Int, minorUnit: String): String =
    if (minor == 0) "$major$majorUnit" else "$major$majorUnit $minor$minorUnit"
