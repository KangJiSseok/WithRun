package com.mobile.watch

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WatchApp(onSendMessage = { sendToPhone() })
        }
    }

    private fun sendToPhone() {
        lifecycleScope.launch {
            try {
                val nodes = Wearable.getNodeClient(this@MainActivity)
                    .connectedNodes
                    .await()

                if (nodes.isEmpty()) {
                    showToast("폰이 연결되지 않음")
                    return@launch
                }

                nodes.forEach { node ->
                    Wearable.getMessageClient(this@MainActivity)
                        .sendMessage(node.id, "/watch-to-phone", "hello from watch".toByteArray())
                        .await()
                }

                showToast("메시지 전송 완료!")
            } catch (e: Exception) {
                showToast("전송 실패: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun WatchApp(onSendMessage: () -> Unit) {
    MaterialTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Button(onClick = onSendMessage) {
                Text(text = "Send")
            }
        }
    }
}
