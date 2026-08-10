package uk.akane.libphonograph.reader

import java.security.MessageDigest

/** Stable, privacy-preserving keys for app-private library state. */
object MediaIdentity {
    fun keys(mediaId: String, path: String): Set<String> = setOf("id:$mediaId", pathKey(path))

    fun pathKey(path: String): String {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(path.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return "path:$hash"
    }
}
