package com.example.sarisari.manager.data.backup

import org.json.JSONException
import org.json.JSONObject

/** The manifest stored as manifest.json inside every backup archive. */
data class BackupManifest(
    val formatVersion: Int,
    val appVersion: String,
    val schemaVersion: Int,
    val createdAtMillis: Long,
    val databaseSha256: String,
) {
    fun toJson(): String = JSONObject()
        .put(KEY_FORMAT, formatVersion)
        .put(KEY_APP_VERSION, appVersion)
        .put(KEY_SCHEMA, schemaVersion)
        .put(KEY_CREATED_AT, createdAtMillis)
        .put(KEY_SHA256, databaseSha256)
        .toString(2)

    companion object {
        const val FORMAT_VERSION = 1
        private const val KEY_FORMAT = "formatVersion"
        private const val KEY_APP_VERSION = "appVersion"
        private const val KEY_SCHEMA = "databaseSchemaVersion"
        private const val KEY_CREATED_AT = "createdAtMillis"
        private const val KEY_SHA256 = "databaseSha256"
        private val SHA256_PATTERN = Regex("^[0-9a-f]{64}$")

        /** Returns null when the text is not a valid version 1 manifest. */
        fun parse(json: String): BackupManifest? = try {
            val obj = JSONObject(json)
            BackupManifest(
                formatVersion = obj.getInt(KEY_FORMAT),
                appVersion = obj.getString(KEY_APP_VERSION),
                schemaVersion = obj.getInt(KEY_SCHEMA),
                createdAtMillis = obj.getLong(KEY_CREATED_AT),
                databaseSha256 = obj.getString(KEY_SHA256),
            ).takeIf { it.formatVersion == FORMAT_VERSION && SHA256_PATTERN.matches(it.databaseSha256) }
        } catch (_: JSONException) {
            null
        }
    }
}
