package com.lagradost.cloudstream3.ui.settings

import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.AppConfig
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream4.viewmodels.GithubFile
import com.lagradost.cloudstream4.viewmodels.GithubRepository
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.FileNotFoundException

object JsonUpdateRepository : GithubRepository {
    private const val TAG = "JsonUpdateRepository"

    @Serializable
    private data class UpdateMetadata(
        @JsonProperty("versionCode") @SerialName("versionCode") val versionCode: Int? = null,
        @JsonProperty("versionName") @SerialName("versionName") val versionName: String? = null,
        @JsonProperty("apkUrl") @SerialName("apkUrl") val apkUrl: String? = null,
        @JsonProperty("releaseNotes") @SerialName("releaseNotes") val releaseNotes: List<String>? = null,
        @JsonProperty("mandatory") @SerialName("mandatory") val mandatory: Boolean = false,
        @JsonProperty("apkSize") @SerialName("apkSize") val apkSize: Long? = null,
        @JsonProperty("sha256") @SerialName("sha256") val sha256: String? = null,
    )

    override suspend fun getSha(userName: String, repository: String, tag: String): String {
        return try {
            GithubReleases.getSha(userName, repository, tag)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get SHA from GitHub: ${e.message}")
            ""
        }
    }

    override suspend fun getRelease(
        userName: String,
        repository: String,
        prerelease: Boolean,
        prereleaseTag: String,
        contentType: String,
    ): GithubFile {
        // If prerelease is requested, we can defer to GitHub releases
        if (prerelease) {
            return GithubReleases.getRelease(
                userName = userName,
                repository = repository,
                prerelease = true,
                prereleaseTag = prereleaseTag,
                contentType = contentType
            )
        }

        // Try primary update URL (Cloudflare R2) and fallback (GitHub raw)
        val urls = listOf(AppConfig.UPDATE_URL, AppConfig.FALLBACK_UPDATE_URL)
        var lastException: Exception? = null

        for (url in urls) {
            try {
                Log.d(TAG, "Fetching update metadata from $url")
                val response = app.get(url)
                val updateData = response.parsed<UpdateMetadata>()

                val apkUrl = updateData.apkUrl
                if (apkUrl.isNullOrBlank()) {
                    throw FileNotFoundException("Missing apkUrl in update manifest at $url")
                }

                val releaseNotesFormatted = updateData.releaseNotes?.joinToString("\n") { line ->
                    if (line.startsWith("- ") || line.startsWith("* ")) line else "- $line"
                } ?: "New update available"

                val digest = updateData.sha256?.trim()?.let { hash ->
                    if (hash.startsWith("sha256:", ignoreCase = true) || hash.startsWith("sha256-", ignoreCase = true)) {
                        hash
                    } else {
                        "sha256:$hash"
                    }
                }

                val versionName = updateData.versionName ?: "v${updateData.versionCode ?: ""}"
                val nodeId = "openstream_v${updateData.versionCode ?: updateData.versionName ?: apkUrl.hashCode()}"

                return GithubFile(
                    digest = digest,
                    downloadUrl = apkUrl,
                    displayName = versionName,
                    changeLog = releaseNotesFormatted,
                    tagName = versionName,
                    nodeId = nodeId,
                    versionCode = updateData.versionCode,
                    mandatory = updateData.mandatory,
                    apkSize = updateData.apkSize
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed fetching update from $url: ${e.message}")
                lastException = e
            }
        }

        // If both URLs fail, throw exception
        throw lastException ?: FileNotFoundException("Unable to fetch update.json from any source")
    }
}
