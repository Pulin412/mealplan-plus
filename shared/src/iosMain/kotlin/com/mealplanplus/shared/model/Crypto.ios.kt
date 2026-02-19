package com.mealplanplus.shared.model

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH

@OptIn(ExperimentalForeignApi::class)
actual fun sha256(input: String): String {
    val data = input.encodeToByteArray()
    val digest = UByteArray(CC_SHA256_DIGEST_LENGTH)

    data.usePinned { pinnedData ->
        digest.usePinned { pinnedDigest ->
            CC_SHA256(
                pinnedData.addressOf(0),
                data.size.convert(),
                pinnedDigest.addressOf(0)
            )
        }
    }

    return digest.joinToString("") { it.toString(16).padStart(2, '0') }
}
