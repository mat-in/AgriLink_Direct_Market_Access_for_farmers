package com.example.agrilink_directmarketaccessforfarmers
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import android.Manifest
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val buyerId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val farmerId = intent.getStringExtra("farmerId") ?: return

        setContent {
            ChatScreen(buyerId, farmerId)
        }
    }
}

@Composable
fun ChatScreen(currentUserId: String, receiverId: String) {
    val database = FirebaseDatabase.getInstance().reference
    val chatId = if (currentUserId < receiverId) "$currentUserId-$receiverId" else "$receiverId-$currentUserId"

    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }

    LaunchedEffect(Unit) {
        val chatRef = database.child("chats").child(chatId).child("messages")
        chatRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messageList = snapshot.children.mapNotNull { it.getValue(ChatMessage::class.java) }
                messages = messageList.sortedBy { it.timestamp }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Chat", "Error loading messages", error.toException())
            }
        })
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(messages) { message ->
                ChatBubble(message, currentUserId)
            }
        }

        ChatInputField(
            onSend = { text -> sendMessage(currentUserId, receiverId, text) },
            onSendLocation = { lat, lon ->
                val locationText = "Location: https://maps.google.com/?q=$lat,$lon"
                sendMessage(currentUserId, receiverId, locationText)
            }
        )
    }
}

@Composable
fun ChatBubble(message: ChatMessage, currentUserId: String) {
    val isSentByUser = message.senderId == currentUserId
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        contentAlignment = if (isSentByUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Text(
            text = message.text,
            color = Color.White,
            modifier = Modifier
                .background(if (isSentByUser) Color.Blue else Color.Gray, shape = RoundedCornerShape(8.dp))
                .padding(8.dp)
        )
    }
}

@Composable
fun ChatInputField(
    onSend: (String) -> Unit,
    onSendLocation: (Double, Double) -> Unit
) {
    var text by remember { mutableStateOf(TextFieldValue("")) }
    val context = LocalContext.current
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") }
            )

            IconButton(onClick = {
                if (text.text.isNotEmpty()) {
                    onSend(text.text)
                    text = TextFieldValue("")
                }
            }) {
                Icon(Icons.Default.Send, contentDescription = "Send Message")
            }
        }

        Button(
            onClick = {
                val permission = Manifest.permission.ACCESS_FINE_LOCATION
                if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            onSendLocation(location.latitude, location.longitude)
                        } else {
                            Toast.makeText(context, "Location not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    ActivityCompat.requestPermissions(context as Activity, arrayOf(permission), 1001)
                }
            },
            modifier = Modifier
                .padding(start = 8.dp, bottom = 8.dp)
                .align(Alignment.Start)
        ) {
            Text("Send Location")
        }
    }
}


fun sendMessage(senderId: String, receiverId: String, messageText: String) {
    val database = FirebaseDatabase.getInstance().reference
    val chatId = if (senderId < receiverId) "$senderId-$receiverId" else "$receiverId-$senderId"

    val message = ChatMessage(senderId, receiverId, messageText, System.currentTimeMillis())

    val chatRef = database.child("chats").child(chatId).child("messages").push()
    chatRef.setValue(message)
        .addOnSuccessListener {
            Log.d("Chat", "Message sent successfully")
        }
        .addOnFailureListener { e ->
            Log.e("Chat", "Failed to send message", e)
        }
}
