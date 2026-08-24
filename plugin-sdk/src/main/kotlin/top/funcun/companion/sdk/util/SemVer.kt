package top.funcun.companion.sdk.util

import kotlinx.serialization.Serializable

/** 语义化版本号 */
@Serializable
data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: String? = null,
) : Comparable<SemVer> {

    constructor(version: String) : this(
        major = version.split(".").getOrNull(0)?.toIntOrNull() ?: 0,
        minor = version.split(".").getOrNull(1)?.toIntOrNull() ?: 0,
        patch = version.split(".").getOrNull(2)?.split("-")?.getOrNull(0)?.toIntOrNull() ?: 0,
        preRelease = version.split("-").getOrNull(1),
    )

    override fun compareTo(other: SemVer): Int {
        return compareValuesBy(
            this, other,
            { it.major },
            { it.minor },
            { it.patch },
            { it.preRelease ?: "" },
        )
    }

    override fun toString(): String {
        return if (preRelease != null) "$major.$minor.$patch-$preRelease"
        else "$major.$minor.$patch"
    }
}
