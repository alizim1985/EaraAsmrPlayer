package com.asmr.player.smb

import android.app.Activity
import android.os.Bundle
import android.provider.DocumentsContract
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import jcifs.smb.SmbException
import java.util.concurrent.Executors

class SmbSettingsActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var store: SmbConfigStore
    private lateinit var displayName: EditText
    private lateinit var host: EditText
    private lateinit var share: EditText
    private lateinit var basePath: EditText
    private lateinit var domain: EditText
    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var status: TextView
    private lateinit var testButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Eara SMB3 設定"
        store = SmbConfigStore(this)
        setContentView(buildUi())
        store.load()?.let { config ->
            displayName.setText(config.displayName)
            host.setText(config.host)
            share.setText(config.share)
            basePath.setText(config.basePath)
            domain.setText(config.domain)
            username.setText(config.username)
            password.setText(config.password)
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun buildUi(): View {
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(24))
        }
        content.addView(TextView(this).apply { text = "SMB2 / SMB3 NAS"; textSize = 24f })
        content.addView(TextView(this).apply { text = "支援 SMB 2.0.2～3.1.1。儲存後在 Eara → 媒體庫 → 加入資料夾，選擇 Eara SMB。" })

        fun field(label: String, hintText: String): EditText {
            content.addView(TextView(this).apply { text = label; setPadding(0, dp(10), 0, 0) })
            return EditText(this).apply {
                hint = hintText
                setSingleLine(true)
                content.addView(this)
            }
        }

        displayName = field("顯示名稱", "我的 NAS")
        host = field("NAS IP / Host", "192.168.4.3")
        share = field("Share 名稱", "大倉NAS")
        basePath = field("起始子資料夾", "music/ASMR")
        domain = field("Domain（通常留白）", "")
        username = field("帳號", "")
        password = field("密碼", "").apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        status = TextView(this).apply { setPadding(0, dp(12), 0, dp(10)) }
        content.addView(status)

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        testButton = Button(this).apply {
            text = "測試連線"
            setOnClickListener { testConnection() }
        }
        buttons.addView(testButton)
        buttons.addView(Button(this).apply {
            text = "儲存"
            setOnClickListener { saveConfig() }
        })
        content.addView(buttons)
        return ScrollView(this).apply { addView(content) }
    }

    private fun readForm() = SmbConfig(
        displayName = displayName.text.toString(),
        host = host.text.toString(),
        share = share.text.toString(),
        basePath = basePath.text.toString(),
        domain = domain.text.toString(),
        username = username.text.toString(),
        password = password.text.toString()
    ).normalized()

    private fun saveConfig() {
        val config = readForm()
        config.validate()?.let { error ->
            status.text = error
            return
        }
        runCatching { store.save(config) }
            .onSuccess {
                runCatching {
                    val authority = packageName + SmbDocumentsProvider.AUTHORITY_SUFFIX
                    contentResolver.notifyChange(DocumentsContract.buildRootsUri(authority), null)
                }
                status.text = "已儲存"
            }
            .onFailure { error ->
                status.text = "儲存失敗：" + (error.message ?: error.javaClass.simpleName)
            }
    }

    private fun testConnection() {
        val config = readForm()
        config.validate()?.let { error ->
            status.text = error
            return
        }
        testButton.isEnabled = false
        status.text = "測試 SMB3 連線中…"
        executor.execute {
            val result = runCatching {
                SmbClientFactory.rootFile(config).use { root ->
                    if (!root.exists()) error("分享不存在")
                    if (!root.isDirectory) error("不是資料夾")
                    "連線成功，可讀取 " + (root.listFiles()?.size ?: 0) + " 個項目"
                }
            }
            runOnUiThread {
                testButton.isEnabled = true
                status.text = result.fold(
                    onSuccess = { it },
                    onFailure = { "連線失敗：" + formatFailure(it) }
                )
            }
        }
    }

    private fun formatFailure(error: Throwable): String {
        val smb = generateSequence(error) { it.cause }
            .filterIsInstance<SmbException>()
            .firstOrNull()
        return if (smb != null) {
            (smb.message ?: smb.javaClass.simpleName) +
                "（NTSTATUS 0x" + "%08X".format(smb.ntStatus) + "）"
        } else {
            error.message ?: error.javaClass.simpleName
        }
    }
}
