@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.example.platform

import kotlin.time.Clock
import kotlinx.datetime.DayOfWeek
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone as KTimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

/*
 * Small multiplatform stand-ins for the handful of java.* / android.* APIs the
 * original Android code used (Log, System.currentTimeMillis, Locale, Date,
 * SimpleDateFormat, Calendar, NumberFormat, String.format). They keep the
 * ported code almost identical to the original while compiling for iOS.
 */

expect object Log {
    fun d(tag: String, msg: String): Int
    fun i(tag: String, msg: String): Int
    fun w(tag: String, msg: String): Int
    fun w(tag: String, msg: String, tr: Throwable?): Int
    fun e(tag: String, msg: String): Int
    fun e(tag: String, msg: String, tr: Throwable?): Int
}

object System {
    fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
    fun nanoTime(): Long = platformNanoTime()
}

expect fun platformNanoTime(): Long

class Locale private constructor(val tag: String) {
    companion object {
        val US = Locale("en-US")
        val ROOT = Locale("")
        val ENGLISH = Locale("en")
        fun getDefault(): Locale = US
        fun forLanguageTag(tag: String): Locale = Locale(tag)
    }
    override fun toString(): String = tag
}

class TimeZone private constructor(val id: String) {
    companion object {
        fun getTimeZone(id: String): TimeZone = TimeZone(id)
        fun getDefault(): TimeZone = TimeZone(KTimeZone.currentSystemDefault().id)
    }
    internal fun toKotlin(): KTimeZone =
        runCatching { KTimeZone.of(if (id == "GMT") "UTC" else id) }.getOrDefault(KTimeZone.UTC)
}

class Date(val time: Long = System.currentTimeMillis()) {
    fun getTime(): Long = time
}

/** Subset of java.text.SimpleDateFormat: y M d H h m s a E EEEE MMM MMMM and quoted literals. */
class SimpleDateFormat(private val pattern: String, @Suppress("UNUSED_PARAMETER") locale: Locale = Locale.US) {
    var timeZone: TimeZone = TimeZone.getDefault()

    fun format(date: Date): String {
        val ldt = Instant.fromEpochMilliseconds(date.time).toLocalDateTime(timeZone.toKotlin())
        return formatLocal(ldt)
    }

    private fun formatLocal(t: LocalDateTime): String {
        val sb = StringBuilder()
        var i = 0
        while (i < pattern.length) {
            val c = pattern[i]
            if (c == '\'') {
                val end = pattern.indexOf('\'', i + 1)
                if (end == i + 1) { sb.append('\''); i += 2; continue }
                if (end < 0) { sb.append(pattern.substring(i + 1)); break }
                sb.append(pattern, i + 1, end); i = end + 1; continue
            }
            if (!c.isLetter()) { sb.append(c); i++; continue }
            var j = i
            while (j < pattern.length && pattern[j] == c) j++
            val n = j - i
            sb.append(field(c, n, t))
            i = j
        }
        return sb.toString()
    }

    private fun pad(v: Int, n: Int) = v.toString().padStart(n, '0')

    private fun field(c: Char, n: Int, t: LocalDateTime): String = when (c) {
        'y' -> if (n == 2) pad(t.year % 100, 2) else pad(t.year, n)
        'M' -> when {
            n >= 4 -> MONTHS[t.monthNumber - 1]
            n == 3 -> MONTHS[t.monthNumber - 1].take(3)
            else -> pad(t.monthNumber, n)
        }
        'd' -> pad(t.dayOfMonth, n)
        'H' -> pad(t.hour, n)
        'k' -> pad(if (t.hour == 0) 24 else t.hour, n)
        'h' -> pad(if (t.hour % 12 == 0) 12 else t.hour % 12, n)
        'K' -> pad(t.hour % 12, n)
        'm' -> pad(t.minute, n)
        's' -> pad(t.second, n)
        'S' -> pad(t.nanosecond / 1_000_000, n.coerceAtLeast(3)).take(n)
        'a' -> if (t.hour < 12) "AM" else "PM"
        'E' -> if (n >= 4) DAYS[t.dayOfWeek.ordinal] else DAYS[t.dayOfWeek.ordinal].take(3)
        'Z', 'X' -> "Z"
        else -> c.toString().repeat(n)
    }

    private companion object {
        val MONTHS = listOf("January", "February", "March", "April", "May", "June", "July",
            "August", "September", "October", "November", "December")
        val DAYS = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    }
}

/** Minimal java.util.Calendar replacement (read-only fields plus add/timeInMillis). */
class Calendar private constructor(var timeInMillis: Long) {
    companion object {
        const val YEAR = 1
        const val MONTH = 2
        const val DAY_OF_MONTH = 5
        const val DAY_OF_YEAR = 6
        const val DAY_OF_WEEK = 7
        const val HOUR_OF_DAY = 11
        const val MINUTE = 12
        const val SECOND = 13
        fun getInstance(): Calendar = Calendar(System.currentTimeMillis())
    }

    private fun local(): LocalDateTime =
        Instant.fromEpochMilliseconds(timeInMillis).toLocalDateTime(KTimeZone.currentSystemDefault())

    fun get(field: Int): Int {
        val t = local()
        return when (field) {
            YEAR -> t.year
            MONTH -> t.monthNumber - 1
            DAY_OF_MONTH -> t.dayOfMonth
            DAY_OF_YEAR -> t.dayOfYear
            // java.util.Calendar: SUNDAY = 1 ... SATURDAY = 7
            DAY_OF_WEEK -> if (t.dayOfWeek == DayOfWeek.SUNDAY) 1 else t.dayOfWeek.ordinal + 2
            HOUR_OF_DAY -> t.hour
            MINUTE -> t.minute
            SECOND -> t.second
            else -> 0
        }
    }

    fun add(field: Int, amount: Int) {
        val delta = when (field) {
            DAY_OF_MONTH, DAY_OF_YEAR, DAY_OF_WEEK -> amount * 86_400_000L
            HOUR_OF_DAY -> amount * 3_600_000L
            MINUTE -> amount * 60_000L
            SECOND -> amount * 1_000L
            else -> 0L
        }
        timeInMillis += delta
    }

    val time: Date get() = Date(timeInMillis)
}

/** java.text.NumberFormat.getNumberInstance(Locale.US) equivalent: grouping + up to 3 decimals. */
class NumberFormat private constructor(private val currency: Boolean) {
    var maximumFractionDigits: Int = if (currency) 2 else 3
    var minimumFractionDigits: Int = if (currency) 2 else 0

    fun format(value: Number): String {
        val d = value.toDouble()
        val body = formatGrouped(abs(d), minimumFractionDigits, maximumFractionDigits)
        val sign = if (d < 0 && body.any { it in '1'..'9' }) "-" else ""
        return if (currency) "$sign$$body" else "$sign$body"
    }

    companion object {
        fun getNumberInstance(@Suppress("UNUSED_PARAMETER") locale: Locale = Locale.US) = NumberFormat(false)
        fun getInstance(@Suppress("UNUSED_PARAMETER") locale: Locale = Locale.US) = NumberFormat(false)
        fun getIntegerInstance(@Suppress("UNUSED_PARAMETER") locale: Locale = Locale.US) =
            NumberFormat(false).apply { maximumFractionDigits = 0 }
        fun getCurrencyInstance(@Suppress("UNUSED_PARAMETER") locale: Locale = Locale.US) = NumberFormat(true)
    }
}

internal fun formatGrouped(value: Double, minFrac: Int, maxFrac: Int, grouping: Boolean = true): String {
    val factor = 10.0.pow(maxFrac)
    val rounded = (value * factor).roundToLong()
    val intPart = rounded / factor.toLong()
    var frac = (rounded % factor.toLong()).toString().padStart(maxFrac, '0')
    if (maxFrac == 0) frac = ""
    while (frac.length > minFrac && frac.endsWith('0')) frac = frac.dropLast(1)
    val intStr = intPart.toString()
    val grouped = if (!grouping) intStr else intStr.reversed().chunked(3).joinToString(",").reversed()
    return if (frac.isEmpty()) grouped else "$grouped.$frac"
}

/** Minimal printf-style formatter supporting %s %d %f %.Nf %,d %,.Nf %0Nd %x %% . */
fun String.Companion.format(@Suppress("UNUSED_PARAMETER") locale: Locale?, format: String, vararg args: Any?): String =
    cFormat(format, args)

fun String.Companion.format(format: String, vararg args: Any?): String = cFormat(format, args)

fun String.format(vararg args: Any?): String = cFormat(this, args)

fun String.format(@Suppress("UNUSED_PARAMETER") locale: Locale?, vararg args: Any?): String = cFormat(this, args)

private val SPEC = Regex("%([-#+ 0,(]*)?(\\d+)?(\\.\\d+)?([sdfxXc%bn])")

private fun cFormat(format: String, args: Array<out Any?>): String {
    var argIndex = 0
    return SPEC.replace(format) { m ->
        val flags = m.groupValues[1]
        val width = m.groupValues[2].toIntOrNull()
        val precision = m.groupValues[3].removePrefix(".").toIntOrNull()
        val conv = m.groupValues[4]
        val out: String = when (conv) {
            "%" -> "%"
            "n" -> "\n"
            else -> {
                val arg = args.getOrNull(argIndex++)
                when (conv) {
                    "d" -> {
                        val v = (arg as? Number)?.toLong() ?: arg.toString().toLongOrNull() ?: 0L
                        val s = if (',' in flags) formatGrouped(abs(v.toDouble()), 0, 0) else abs(v).toString()
                        (if (v < 0) "-" else if ('+' in flags) "+" else "") + s
                    }
                    "f" -> {
                        val v = (arg as? Number)?.toDouble() ?: arg.toString().toDoubleOrNull() ?: 0.0
                        val p = precision ?: 6
                        val s = formatGrouped(abs(v), p, p, grouping = ',' in flags)
                        (if (v < 0 && s.any { it in '1'..'9' }) "-" else if ('+' in flags) "+" else "") + s
                    }
                    "x" -> ((arg as? Number)?.toLong() ?: 0L).toString(16)
                    "X" -> ((arg as? Number)?.toLong() ?: 0L).toString(16).uppercase()
                    "c" -> arg.toString().take(1)
                    "b" -> (arg != null && arg != false).toString()
                    else -> {
                        val s = arg.toString()
                        if (precision != null) s.take(precision) else s
                    }
                }
            }
        }
        if (width != null && out.length < width) {
            when {
                '-' in flags -> out.padEnd(width)
                '0' in flags && conv in "dfxX" -> {
                    val neg = out.startsWith("-")
                    (if (neg) "-" else "") + out.removePrefix("-").padStart(width - (if (neg) 1 else 0), '0')
                }
                else -> out.padStart(width)
            }
        } else out
    }
}
