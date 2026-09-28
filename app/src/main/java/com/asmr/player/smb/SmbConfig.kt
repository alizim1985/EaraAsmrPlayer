package com.asmr.player.smb

data class SmbConfig(
    val displayName: String = "NAS",
    val host: String,
    val share: String,
    val basePath: String = "",
    val domain: String = "",
    val username: String = "",
    val password: String = ""
) {
    fun normalized(): SmbConfig = copy(
        displayName = displayName.trim().ifBlank { "NAS" },
        host = host.trim().removePrefix("smb://").trim('/'),
        share = share.trim().trim('/'),
        basePath = basePath.trim().trim('/'),
        domain = domain.trim(),
        username = username.trim()
    )

    fun validate(): String? {
        val cfg = normalized()
        if (cfg.host.isBlank()) return "主機/IP 不可空白"
        if (cfg.share.isBlank()) return "SMB 分享名稱不可空白"
        if (cfg.host.contains('/')) return "主機請只填 IP 或主機名稱，不要包含資料夾路徑"
        return null
    }
}
