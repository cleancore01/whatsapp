package com.cleancore.whatsapp

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import okhttp3.*

data class Recipient(val number: String, var status: String = "Pending")

class MainActivity : AppCompatActivity() {
    private val list = mutableListOf<Recipient>()
    private val client = OkHttpClient()
    private lateinit var message: EditText
    private lateinit var link: EditText
    private lateinit var api: EditText
    private lateinit var key: EditText
    private lateinit var number: EditText
    private lateinit var rows: LinearLayout
    private lateinit var progress: ProgressBar
    private lateinit var count: TextView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContentView(ui())
    }

    private fun ui(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        fun e(h: String) = EditText(this).apply { hint = h }
        message = e("Message").also { it.minLines = 4 }
        link = e("Link")
        api = e("Sender API URL")
        key = e("API key")
        number = e("+91 number")

        root.addView(message, lp())
        root.addView(link, lp())
        root.addView(api, lp())
        root.addView(key, lp())

        val add = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        add.addView(number, LinearLayout.LayoutParams(0, -2, 1f))
        add.addView(Button(this).apply {
            text = "Add"
            setOnClickListener { addNumber() }
        })
        root.addView(add, lp())

        rows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(rows) }, LinearLayout.LayoutParams(-1, 0, 1f))

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        root.addView(progress, lp())
        count = TextView(this).apply { text = "0 / 0" }
        root.addView(count, lp())
        root.addView(Button(this).apply {
            text = "START"
            setOnClickListener { startSending() }
        })
        return root
    }

    private fun lp() = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 8 }

    private fun addNumber() {
        val n = number.text.toString().trim().replace(Regex("[^0-9+]"), "")
        if (n.isBlank() || list.any { it.number == n }) return
        list.add(Recipient(n))
        number.text.clear()
        render()
    }

    private fun render() {
        rows.removeAllViews()
        list.forEachIndexed { i, r ->
            rows.addView(TextView(this).apply {
                text = "\${i + 1}. \${r.number} — \${r.status}"
                textSize = 16f
            }, lp())
        }
    }

    private fun startSending() {
        if (list.isEmpty()) return
        val base = api.text.toString().trim().trimEnd('/')
        val token = key.text.toString().trim()
        val msg = message.text.toString()
        val urlLink = link.text.toString()

        CoroutineScope(Dispatchers.IO).launch {
            list.forEachIndexed { i, r ->
                val body = FormBody.Builder()
                    .add("to", r.number)
                    .add("message", msg)
                    .add("link", urlLink)
                    .build()
                val req = Request.Builder()
                    .url("\$base/send")
                    .addHeader("Authorization", "Bearer \$token")
                    .post(body)
                    .build()

                r.status = try {
                    client.newCall(req).execute().use {
                        if (it.isSuccessful) "Sent" else "Failed (\${it.code})"
                    }
                } catch (_: Exception) {
                    "Failed"
                }

                withContext(Dispatchers.Main) {
                    progress.progress = ((i + 1) * 100) / list.size
                    count.text = "\${i + 1} / \${list.size}"
                    render()
                }
                delay(1500)
            }
        }
    }
}
