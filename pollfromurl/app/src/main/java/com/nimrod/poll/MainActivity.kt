package com.nimrod.poll

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.webkit.URLUtil
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setup()
    }

    private fun setup() {
        enter_url.onDone {
            onStartServiceClicked()
        }

        start_service.setOnClickListener {
            onStartServiceClicked()
        }

        stop_service.setOnClickListener {
            onStopService()
        }
    }

    private fun onStartServiceClicked() {
        val text = enter_url.text.toString()
        if (text.isValidUrl()) {
            startService(text)
        } else {
            Toast.makeText(this, getString(R.string.start_service_error_toast), Toast.LENGTH_LONG).show()
        }
    }

    private fun onStopService() {
        val serviceIntent = Intent(this, PollService::class.java)
        stopService(serviceIntent)
        Toast.makeText(this, getString(R.string.stop_service_toast), Toast.LENGTH_LONG).show()
    }

    private fun startService(url: String) {
        val serviceIntent = Intent(this, PollService::class.java)
        serviceIntent.putExtra(URL_EXTRA, url)
        ContextCompat.startForegroundService(this, serviceIntent)
        Toast.makeText(this, getString(R.string.start_service_toast), Toast.LENGTH_LONG).show()
    }

    private fun EditText.onDone(callback: () -> Unit) {
        // These lines optional if you don't want to set in Xml
        imeOptions = EditorInfo.IME_ACTION_DONE
        maxLines = 1
        setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                callback.invoke()
                true
            }
            false
        }
    }

    companion object {
        const val URL_EXTRA = "extra_poll_url"
    }
}

fun String?.isValidUrl(): Boolean {
    return URLUtil.isHttpUrl(this) || URLUtil.isHttpsUrl(this)
}