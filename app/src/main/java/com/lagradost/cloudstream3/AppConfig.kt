package com.lagradost.cloudstream3

object AppConfig {
    const val APP_NAME = "Openstream"
    const val DEFAULT_REPOSITORY_URL = "https://raw.githubusercontent.com/nehalDIU/nehal-CloudStream/master/repo.json"
    const val FALLBACK_REPOSITORY_URL = "https://cdn.jsdelivr.net/gh/nehalDIU/nehal-CloudStream@master/repo.json"
    const val DEFAULT_REPOSITORY_NAME = "Openstream Repository"

    val VERSION: String get() = BuildConfig.VERSION_NAME

    // Update Configurations
    const val UPDATE_URL = "https://pub-e9a64ec2e4424ee6b04d9dac6a883b3b.r2.dev/update.json"
    const val FALLBACK_UPDATE_URL = "https://raw.githubusercontent.com/sheikhshariarnehal/upstream/master/update.json"

    // Default Settings & Retry Configurations
    const val MAX_RETRIES = 4
    const val RETRY_DELAY_MS = 1500L
    const val LOG_TAG = "Openstream"
}
