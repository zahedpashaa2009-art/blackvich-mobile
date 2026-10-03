package com.blackvich.pos

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import android.webkit.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream

/**
 * Black Vich Mobile — کلاینت موبایل.
 * هیچ داده‌ای روی گوشی نیست؛ همه‌چیز از سرور روی کامپیوتر (پورت ۸۰۰۰) می‌آید.
 * صفحه‌ی ورود (assets/connect.html) لینک یا QR را می‌گیرد و بعد صفحات سفارش/آشپزخانه/مدیریت
 * همان سرور داخل برنامه باز می‌شوند، پس همه‌ی امکانات دقیقاً مثل نسخه‌ی ویندوز است.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var web: WebView
    private var serverOrigin: String? = null
    private val prefs by lazy { getSharedPreferences("bv", Context.MODE_PRIVATE) }

    private val scanLauncher = registerForActivityResult(ScanContract()) { r ->
        val text = r.contents
        if (text != null) js("onScanResult(" + JSONObject.quote(text) + ")")
        else js("onScanCancel()")
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        web = WebView(this)
        web.setBackgroundColor(Color.parseColor("#060504"))
        setContentView(web)
        ViewCompat.setOnApplyWindowInsetsListener(web) { v, ins ->
            val b = ins.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, b.top, 0, 0)   // پایین را خود نوار شناور رعایت می‌کند
            ins
        }

        with(web.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false   // بوق سفارش جدید در آشپزخانه
            allowFileAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            textZoom = 100
        }
        web.addJavascriptInterface(Bridge(), "Android")
        web.webChromeClient = WebChromeClient()
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(v: WebView, req: WebResourceRequest): WebResourceResponse? {
                val u = req.url
                // فونت وزیرمتن از داخل برنامه (بدون اینترنت)
                if (u.host == "appassets.local" && u.path?.startsWith("/fonts/") == true) {
                    return try {
                        // هر فایل فونتی که داخل assets/fonts باشد (با هر اسمی) به‌عنوان وزیرمتن استفاده می‌شود
                        val f = assets.list("fonts")?.firstOrNull { it.endsWith(".woff2") || it.endsWith(".ttf") }
                            ?: return null
                        val s = assets.open("fonts/" + f)
                        WebResourceResponse(if (f.endsWith(".ttf")) "font/ttf" else "font/woff2", null, s).apply {
                            responseHeaders = mapOf("Access-Control-Allow-Origin" to "*")
                        }
                    } catch (e: Exception) { null }
                }
                // CDN فونت بدون اینترنت معطل نکند
                if (u.host == "cdn.jsdelivr.net" && u.toString().contains("vazirmatn", true)) {
                    return WebResourceResponse("text/css", "utf-8", ByteArrayInputStream(ByteArray(0)))
                }
                return null
            }

            override fun shouldOverrideUrlLoading(v: WebView, req: WebResourceRequest): Boolean {
                // اجازه‌ی رفتن به صفحات سفارش/مدیریت داده نمی‌شود
                val u = req.url
                val base = serverOrigin ?: return false
                if (u.scheme == "http" && base.endsWith(u.authority ?: "") && !(u.path ?: "").startsWith("/kitchen")) return true
                return false
            }

            override fun onPageFinished(v: WebView, url: String) {
                if (url.startsWith("http")) injectShell()
            }

            override fun onReceivedError(v: WebView, r: WebResourceRequest, e: WebResourceError) {
                if (r.isForMainFrame && serverOrigin != null) {
                    val msg = "اتصال به کامپیوتر قطع شد. مطمئن شو برنامه روی کامپیوتر روشن است و هر دو روی یک وای‌فای هستید."
                    serverOrigin = null
                    web.loadUrl("file:///android_asset/connect.html?err=" + Uri.encode(msg))
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (serverOrigin != null && web.canGoBack()) web.goBack()
                else if (serverOrigin != null) js("window.__bvConfirmExit && window.__bvConfirmExit()")
                else finish()
            }
        })

        val auto = prefs.getString("last", null) != null && prefs.getBoolean("auto", true)
        web.loadUrl("file:///android_asset/connect.html" + if (auto) "?auto=1" else "")
    }

    private fun js(code: String) = runOnUiThread { web.evaluateJavascript(code, null) }

    /** نوار پایین + دکمه‌ی قطع اتصال روی صفحات سرور تزریق می‌شود */
    private fun injectShell() {
        val code = assets.open("shell.js").bufferedReader().use { it.readText() }
        web.evaluateJavascript(code, null)
    }

    inner class Bridge {
        @JavascriptInterface fun scan() = runOnUiThread {
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.CAMERA), 7)
            } else launchScan()
        }

        @JavascriptInterface fun getHistory(): String = prefs.getString("history", "[]") ?: "[]"
        @JavascriptInterface fun getLast(): String = prefs.getString("last", "") ?: ""

        @JavascriptInterface fun connect(origin: String, page: String) = runOnUiThread {
            serverOrigin = origin
            val h = try { JSONArray(prefs.getString("history", "[]")) } catch (e: Exception) { JSONArray() }
            val out = JSONArray().put(origin)
            for (i in 0 until h.length()) if (h.getString(i) != origin && out.length() < 5) out.put(h.getString(i))
            prefs.edit().putString("history", out.toString()).putString("last", origin).putBoolean("auto", true).apply()
            web.loadUrl(origin + "/kitchen")   // این نسخه فقط آشپزخانه است
        }

        @JavascriptInterface fun disconnect() = runOnUiThread {
            serverOrigin = null
            prefs.edit().putBoolean("auto", false).apply()
            web.clearHistory()
            web.loadUrl("file:///android_asset/connect.html")
        }

        @JavascriptInterface fun forget(origin: String) {
            val h = try { JSONArray(prefs.getString("history", "[]")) } catch (e: Exception) { JSONArray() }
            val out = JSONArray()
            for (i in 0 until h.length()) if (h.getString(i) != origin) out.put(h.getString(i))
            prefs.edit().putString("history", out.toString()).apply()
        }

        @JavascriptInterface fun haptic(ms: Int) {
            val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(ms.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
            else @Suppress("DEPRECATION") v.vibrate(ms.toLong())
        }

        @JavascriptInterface fun exit() = runOnUiThread { finish() }
    }

    private fun launchScan() {
        scanLauncher.launch(
            ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt("QR نمایش‌داده‌شده روی کامپیوتر را اسکن کن")
                .setBeepEnabled(false)
                .setOrientationLocked(false)
        )
    }

    override fun onRequestPermissionsResult(code: Int, p: Array<out String>, g: IntArray) {
        super.onRequestPermissionsResult(code, p, g)
        if (code == 7) {
            if (g.isNotEmpty() && g[0] == PackageManager.PERMISSION_GRANTED) launchScan()
            else js("onScanDenied()")
        }
    }
}
