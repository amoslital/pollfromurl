package com.nimrod.poll

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.*
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

class PollService : Service() {
    private var isRunning = false
    private var url: String? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra(MainActivity.URL_EXTRA)
        val isValidUrl = url?.isValidUrl() == true
        val contentText ="Polling from $url".takeIf { isValidUrl } ?: "Not Polling"

        createNotificationChannel(FOREGROUND_CHANNEL_ID, "Poll Channel")
        createNotificationChannel(RESPONSE_CHANNEL_ID, "Poll Response")

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, 0
        )
        val notification: Notification = NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
            .setContentTitle("Nimrod's Poll Service")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1, notification)

        if (url != null && isValidUrl) {
            var startPolling = false
            if (this.url == null) {
                startPolling = true
            }
            this.url = url

            if (startPolling) {
                GlobalScope.launch(Dispatchers.Main){ // launches coroutine in main thread
                    startPolling()
                }
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }

    private fun createNotificationChannel(channelId: String, channelName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(
                NotificationManager::class.java
            )
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private suspend fun startPolling() {
        isRunning = true
        while (isRunning) {
            val response = GlobalScope.async(Dispatchers.IO) {
                delay(2000L)
                url?.let {
                    poll(it)
                }
            }
            notifyResponse(response.await())
        }

    }

    private fun poll(urlString: String): String? {
        val url = URL(urlString)
        var urlConnection: HttpURLConnection? = null
        var response: String? = null
        try {
            urlConnection = url.openConnection() as? HttpURLConnection
            urlConnection?.doInput = true;

            val inputStream: InputStream? = urlConnection?.inputStream
            response = readStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            urlConnection?.disconnect()
        }

        return response
    }

    private fun readStream(inputStream: InputStream?): String? {
        var reader: BufferedReader? = null
        val response = StringBuffer()
        try {
            reader = BufferedReader(InputStreamReader(inputStream))
            var line: String? = ""
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (reader != null) {
                try {
                    reader.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return response.toString()
    }

    private fun notifyResponse(response: String?) {
        val message = response.takeIf { !response.isNullOrBlank() } ?: "Empty Response"
        val builder = NotificationCompat.Builder(this, RESPONSE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle("Poll Response Received")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            notify(+id, builder.build())
        }
    }

    companion object {
        private var id = 10
        private const val FOREGROUND_CHANNEL_ID = "PollServiceChannel"
        private const val RESPONSE_CHANNEL_ID = "PollServiceResponseChannel"
    }
}
