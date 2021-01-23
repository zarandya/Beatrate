package io.github.zarandya.beatrate

import java.util.stream.Collectors
import java.util.stream.Stream

fun parseTargetRatePreferenceString(s: String): ArrayList<Double> {
    return ArrayList(s.split(";").mapNotNull { it.toDoubleOrNull() })
}

fun serialiseTargetRates(value: ArrayList<Double>): String {
    return value.joinToString(separator = ";")
}
