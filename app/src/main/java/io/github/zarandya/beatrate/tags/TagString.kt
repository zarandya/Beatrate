package io.github.zarandya.beatrate.tags

import android.util.Base64.*
import com.poupa.vinylmusicplayer.util.PreferenceUtil
import java.lang.Exception
import java.security.MessageDigest
import java.util.*

private const val TAG = "@io.github.zarandya.Beatrate.TAG_STRING"

private val regex = Regex("$TAG\\(([0-9a-zA-Z=+/]*),([0-9]*),([0-9.]*),([0-9a-zA-Z_\\-]*)\\)")
private val md = MessageDigest.getInstance("SHA-256")

fun generateTagSignature(bpmString: String, bpmType: Int): String {
    val salt = UUID.randomUUID().toString()
    val instanceId = PreferenceUtil.getInstance().instanceId
    val beatrateTagString = "$bpmType,$bpmString,$salt"
    val hash = encodeToString(md.digest("$instanceId,$beatrateTagString".toByteArray()), NO_WRAP)
    return "$TAG($hash,$beatrateTagString)"
}

fun updateTagSignature(originalTagString: String, bpmString: String, bpmType: Int): String {
    val newTagSignature = generateTagSignature(bpmString, bpmType)
    if (originalTagString.contains(regex)) {
        return originalTagString.replace(regex, newTagSignature)
    }
    return "$originalTagString $newTagSignature"
}

fun getBpmTypeIfHasValidTagSignature(tagString: String, bpmString: String): Int? {
    val match = regex.find(tagString) ?.groupValues ?: return null
    try {
        val hash = match[1]
        val bpmType = match[2]
        val bpmStringParsed = match[3]
        val salt = match[4]
        val beatrateTagString = "$bpmType,$bpmString,$salt"
        val instanceId = PreferenceUtil.getInstance().instanceId

        if (bpmStringParsed != bpmString) return null
        
        if (hash != encodeToString(md.digest("$instanceId,$beatrateTagString".toByteArray()), NO_WRAP))
            return null

        return bpmType.toInt()
    }
    catch (e: Exception) {
        return null
    }
}