package com.sdpromptviewer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.webkit.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            loadApp()
        } else {
            showPermissionDenied()
        }
    }

    private val requestReadPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) loadApp() else showPermissionDenied() }

    private val requestMediaPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) loadApp() else showPermissionDenied() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webView)
        setupWebView()
        checkPermissionsAndLoad()
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            allowFileAccess = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        webView.addJavascriptInterface(FileBridge(), "AndroidBridge")
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView, request: WebResourceRequest
            ): WebResourceResponse? {
                if (request.url.scheme == "localfile") {
                    val path = request.url.path ?: return null
                    return try {
                        val file = File(path)
                        if (!file.exists() || !file.isFile) return null
                        val mime = when (file.extension.lowercase()) {
                            "png" -> "image/png"
                            "jpg", "jpeg" -> "image/jpeg"
                            "webp" -> "image/webp"
                            else -> "application/octet-stream"
                        }
                        val resp = WebResourceResponse(mime, null, file.inputStream())
                        resp.responseHeaders = mapOf(
                            "Access-Control-Allow-Origin" to "*",
                            "Cache-Control" to "max-age=3600"
                        )
                        resp
                    } catch (e: Exception) { null }
                }
                return null
            }
        }
    }

    private fun checkPermissionsAndLoad() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                if (Environment.isExternalStorageManager()) loadApp()
                else manageStorageLauncher.launch(
                    Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED) loadApp()
                else requestMediaPermission.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
            else -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) loadApp()
                else requestReadPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun loadApp() {
        webView.loadUrl("file:///android_asset/index.html")
    }

    private fun showPermissionDenied() {
        webView.loadData(
            "<html><body style='background:#0d0d0d;color:#e8e8e8;font-family:sans-serif;" +
            "padding:40px;text-align:center'><h2>ストレージの権限が必要です</h2>" +
            "<p style='margin-top:16px'>設定 → アプリ → SD Prompt Viewer → 権限<br>" +
            "→ ファイルとメディア → すべてのファイルにアクセス</p></body></html>",
            "text/html", "utf-8"
        )
    }

    inner class FileBridge {

        @JavascriptInterface
        fun listImages(path: String): String {
            return try {
                val dir = File(path)
                if (!dir.exists() || !dir.isDirectory) return "[]"
                val files = dir.listFiles()
                    ?.filter { it.isFile && it.name.matches(Regex("(?i).*\\.(png|jpe?g|webp)$")) }
                    ?.sortedByDescending { it.lastModified() }
                    ?: return "[]"
                JSONArray(files.map { f ->
                    JSONObject().apply {
                        put("name", f.name)
                        put("path", f.absolutePath)
                        put("size", f.length())
                        put("modified", f.lastModified())
                    }
                }).toString()
            } catch (e: Exception) { "[]" }
        }

        @JavascriptInterface
        fun listDirs(path: String): String {
            return try {
                val dir = File(path)
                if (!dir.exists() || !dir.isDirectory) return "[]"
                val dirs = dir.listFiles()
                    ?.filter { it.isDirectory }
                    ?.sortedWith(compareBy({ !it.name.startsWith(".") }, { it.name.lowercase() }))
                    ?: return "[]"
                JSONArray(dirs.map { d ->
                    JSONObject().apply {
                        put("name", d.name)
                        put("path", d.absolutePath)
                        put("hidden", d.name.startsWith("."))
                        put("imageCount", d.listFiles()
                            ?.count { it.isFile && it.name.matches(Regex("(?i).*\\.(png|jpe?g|webp)$")) } ?: 0)
                    }
                }).toString()
            } catch (e: Exception) { "[]" }
        }

        @JavascriptInterface
        fun readFileBase64(path: String): String {
            return try {
                val file = File(path)
                if (!file.exists()) return ""
                val readSize = minOf(file.length(), 512L * 1024L).toInt()
                val bytes = ByteArray(readSize)
                file.inputStream().use { it.read(bytes) }
                android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            } catch (e: Exception) { "" }
        }

        @JavascriptInterface
        fun getPicturesPath(): String =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath

        @JavascriptInterface
        fun exists(path: String): Boolean = File(path).exists()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }
}
