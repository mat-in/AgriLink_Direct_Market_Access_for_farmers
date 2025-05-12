package com.example.agrilink_directmarketaccessforfarmers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.agrilink_directmarketaccessforfarmers.ui.theme.AgriLinkDirectMarketAccessForFarmersTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FarmerDashboardActivity : ComponentActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val database = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgriLinkDirectMarketAccessForFarmersTheme {
                FarmerDashboardScreen()
            }
        }
    }


    @Composable
    fun FarmerDashboardScreen() {
        var selectedScreen by remember { mutableStateOf(0) }
        val screens = listOf("Post", "Feed", "Profile", "Settings")
        val icons = listOf(
            Icons.Default.AddCircle,
            Icons.Default.FavoriteBorder,
            Icons.Default.Person,
            Icons.Default.Settings
        )

        Scaffold(
            bottomBar = {
                NavigationBar {
                    screens.forEachIndexed { index, title ->
                        NavigationBarItem(
                            selected = selectedScreen == index,
                            onClick = { selectedScreen = index },
                            icon = { Icon(imageVector = icons[index], contentDescription = title) },
                            label = { Text(title) }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                when (selectedScreen) {
                    0 -> PostProductScreen()
                    1 -> FarmerFeedScreen()
                    2 -> ProfileScreen()
                    3 -> SettingsScreen()
                }
            }
        }
    }

    @Composable
    fun PostProductScreen() {
        var productName by remember { mutableStateOf("") }
        var price by remember { mutableStateOf("") }
        val images = remember { mutableStateListOf<Uri>() }
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetMultipleContents(),
            onResult = { uris ->
                images.addAll(uris)
                if (images.size > 3) {
                    images.removeAll(images.subList(3, images.size))
                }
            }
        )

        val context = LocalContext.current // Get context within Composable
        val user = auth.currentUser

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Post New Product")
            OutlinedTextField(
                value = productName,
                onValueChange = { productName = it },
                label = { Text("Product Name") }
            )
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("Price") }
            )

            Row {
                Button(onClick = { launcher.launch("image/*") }) {
                    Text("Select Images")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row {
                images.forEach { uri ->
                    val bitmap = remember { mutableStateOf<android.graphics.Bitmap?>(null) }
                    val contentResolver = context.contentResolver // Use context
                    try {
                        val input = contentResolver.openInputStream(uri)
                        bitmap.value = android.graphics.BitmapFactory.decodeStream(input)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    bitmap.value?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Selected Image",
                            modifier = Modifier.size(100.dp).padding(4.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Button(
                onClick = {
                    if (productName.isNotEmpty() && price.isNotEmpty()) {
                        val base64Images = mutableListOf<String>()
                        images.forEach { uri ->
                            base64Images.add(uriToBase64(uri, context)) // Pass context
                        }
                        val product = hashMapOf(
                            "name" to productName,
                            "price" to price,
                            "timestamp" to System.currentTimeMillis(),
                            "images" to base64Images,
                            "userId" to user?.uid //add the user id.
                        )

                        firestore.collection("products").add(product)
                            .addOnSuccessListener {
                                Log.d("Firestore", "Product posted successfully!")
                                productName = ""
                                price = ""
                                images.clear()
                            }
                            .addOnFailureListener { e ->
                                Log.e("Firestore", "Error posting product", e)
                            }
                    }
                }
            ) {
                Text("Post")
            }
        }
    }

    fun uriToBase64(uri: Uri, context: Context): String {
        val inputStream = context.contentResolver.openInputStream(uri)
        val byteArrayOutputStream = ByteArrayOutputStream()
        inputStream?.use { input ->
            input.copyTo(byteArrayOutputStream)
        }
        return Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT)
    }

    @Composable
    fun FarmerFeedScreen() {
        var products by remember { mutableStateOf<List<Pair<Product, Pair<String, String?>>>>(emptyList()) }
        val firestore = FirebaseFirestore.getInstance()

        LaunchedEffect(Unit) {
            firestore.collection("products")
                .orderBy("timestamp")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("Firestore", "Error fetching products", e)
                        return@addSnapshotListener
                    }
                    val productList = snapshot?.documents?.map { document ->
                        val product = Product(
                            name = document.getString("name") ?: "Unknown",
                            price = document.getString("price") ?: "0",
                            images = document.get("images") as? List<String> ?: emptyList(),
                            userId = document.getString("userId") ?: ""
                        )
                        product
                    } ?: emptyList()

                    val productUserPair = mutableListOf<Pair<Product, Pair<String,String?>>>()

                    productList.forEach{product ->
                        firestore.collection("users").document(product.userId).get().addOnSuccessListener{ userDocument ->
                            val userName = userDocument.getString("name") ?: "unknown"
                            val userPhoto = userDocument.getString("photoUrl")
                            productUserPair.add(Pair(product, Pair(userName, userPhoto)))
                            products = productUserPair.toList()
                        }.addOnFailureListener{e ->
                            Log.e("Firestore", "Error fetching user data", e)
                        }
                    }
                }
        }

        Column {
            Text("Farmer Feed - View Products", modifier = Modifier.padding(16.dp))
            LazyColumn {
                items(products) { (product, userDetails) ->
                    ProductCardWithUserName(product, userDetails.first, userDetails.second)
                }
            }
        }
    }

    @Composable
    fun <T> ProductCardWithUserName(product: T, userName: String, photoUrl: String?, onMessageClick: (() -> Unit)? = null) {
        Card(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (photoUrl != null) {
                            Image(
                                painter = rememberAsyncImagePainter(model = photoUrl),
                                contentDescription = "Seller Photo",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Person, contentDescription = "Seller Photo", modifier = Modifier.size(40.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Seller: $userName", style = MaterialTheme.typography.bodySmall)
                    }
                    onMessageClick?.let {
                        IconButton(onClick = it) {
                            Icon(Icons.Filled.Email, contentDescription = "Message Seller")
                        }
                    }
                }
                Text(text = "Product: ${if (product is Product) product.name else (product as ProductListing).name}", style = MaterialTheme.typography.bodyLarge)
                Text(text = "Price: ${if (product is Product) product.price else (product as ProductListing).price}", style = MaterialTheme.typography.bodyMedium)
                ProductCard(product = if (product is Product) product else (product as ProductListing))
            }
        }
    }

    @Composable
    fun ProfileScreen() {
        val user = auth.currentUser
        var userName by remember { mutableStateOf("") }
        var userPhotoUrl by remember { mutableStateOf<String?>(null) }
        var userPosts by remember { mutableStateOf<List<Product>>(emptyList()) }
        var showChatList by remember { mutableStateOf(false) } // State for showing chat list

        LaunchedEffect(Unit) {
            user?.let {
                userName = it.displayName ?: "User Name"
                userPhotoUrl = it.photoUrl?.toString()

                firestore.collection("products")
                    .whereEqualTo("userId", it.uid)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            Log.e("Firestore", "Error fetching user posts", e)
                            return@addSnapshotListener
                        }
                        val posts = snapshot?.documents?.map { document ->
                            Product(
                                name = document.getString("name") ?: "Unknown",
                                price = document.getString("price") ?: "0",
                                images = document.get("images") as? List<String> ?: emptyList(),
                                userId = document.getString("userId") ?: "" // Include userId
                            )
                        } ?: emptyList()
                        userPosts = posts
                    }

                firestore.collection("users").document(it.uid).get().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val document = task.result
                        if (!document.exists()) {
                            val userMap = hashMapOf("name" to userName)
                            firestore.collection("users").document(it.uid).set(userMap).addOnSuccessListener {
                                Log.d("Firestore", "User collection Created")
                            }.addOnFailureListener { e ->
                                Log.e("Firestore", "Error creating user document", e)
                            }
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (userPhotoUrl != null) {
                        Image(
                            painter = rememberAsyncImagePainter(userPhotoUrl),
                            contentDescription = "Profile Photo",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = "Profile Photo", modifier = Modifier.size(80.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(userName, style = MaterialTheme.typography.headlineSmall)
                }

                IconButton(onClick = { showChatList = true }) {
                    Icon(Icons.Filled.Email, contentDescription = "Messages")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Your Posts:", style = MaterialTheme.typography.headlineSmall)
            LazyColumn {
                items(userPosts) { product ->
                    ProductCard(product)
                }
            }
        }

        val context = LocalContext.current

        if (showChatList) {
            ChatListDialog(
                onDismiss = { showChatList = false },
                onChatClicked = { receiverId ->
                    showChatList = false
                    val intent = Intent(context, ChatActivity::class.java)
                    intent.putExtra("farmerId", receiverId)
                    context.startActivity(intent)
                }
            )
        }

    }

    @Composable
    fun ChatListDialog(
        onDismiss: () -> Unit,
        onChatClicked: (receiverId: String) -> Unit
    ) {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser ?: return
        val firestore = FirebaseFirestore.getInstance()
        val realtimeDB = FirebaseDatabase.getInstance().reference

        var chatUsers by remember { mutableStateOf<List<ChatUser>>(emptyList()) }

        LaunchedEffect(Unit) {
            firestore.collection("users").get().addOnSuccessListener { result ->
                val fetchedUsers = mutableListOf<ChatUser>()

                result.documents.forEach { doc ->
                    val uid = doc.id
                    if (uid != currentUser.uid) {
                        val name = doc.getString("name") ?: "Unnamed"
                        val photoUrl = doc.getString("photoUrl")

                        val chatId = if (currentUser.uid < uid) "${currentUser.uid}-$uid" else "$uid-${currentUser.uid}"
                        realtimeDB.child("chats").child(chatId).child("messages")
                            .limitToLast(1)
                            .get()
                            .addOnSuccessListener { messagesSnapshot ->
                                val lastMsg = messagesSnapshot.children.firstOrNull()?.getValue(ChatMessage::class.java)?.text ?: ""
                                fetchedUsers.add(ChatUser(uid, name, photoUrl, lastMsg))
                                chatUsers = fetchedUsers.sortedByDescending { it.lastMessage } // optional: recent at top
                            }
                    }
                }
            }
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Chats") },
            text = {
                LazyColumn {
                    items(chatUsers) { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onChatClicked(user.userId) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (user.photoUrl != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(user.photoUrl),
                                    contentDescription = "Profile Pic",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(40.dp))
                            }

                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(user.name, style = MaterialTheme.typography.bodyLarge)
                                Text(user.lastMessage, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )
    }



    @Composable
    fun ChatListItem(user: ChatUser, onClick: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (user.photoUrl != null) {
                Image(
                    painter = rememberAsyncImagePainter(user.photoUrl),
                    contentDescription = "User Photo",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Person, contentDescription = "User Photo", modifier = Modifier.size(40.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(user.name, style = MaterialTheme.typography.bodyLarge)
                Text(user.lastMessage, style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    data class ChatUser(
        val userId: String,
        val name: String,
        val photoUrl: String?,
        val lastMessage: String
    )


    @Composable
    fun SettingsScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(title = "Notifications") {
                // Implement notification settings here
                Text("Notification Settings (Placeholder)")
            }

            SettingsItem(title = "Privacy") {
                // Implement privacy settings here
                Text("Privacy Settings (Placeholder)")
            }

            SettingsItem(title = "Account") {
                // Implement account settings here
                Text("Account Settings (Placeholder)")
            }

            SettingsItem(title = "Language") {
                // Implement language settings here
                Text("Language Settings (Placeholder)")
            }

            SettingsItem(title = "Theme") {
                // Implement theme settings here
                Text("Theme Settings (Placeholder)")
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsItem(title = "About Us") {
                AboutUsScreen()
            }

            SettingsItem(title = "Contact Us") {
                ContactUsScreen()
            }
        }
    }

    @Composable
    fun SettingsItem(title: String, content: @Composable () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { /* Handle item click */ }
                .padding(vertical = 8.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            content()
        }
    }

    @Composable
    fun AboutUsScreen() {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = "About Us")
                Text("About AgriLink", style = MaterialTheme.typography.headlineSmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "AgriLink is a platform connecting farmers and buyers directly. " +
                        "Our mission is to empower farmers by providing them with direct access to markets " +
                        "and to ensure buyers have access to fresh, quality produce.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Version: 1.0.0",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    @Composable
    fun ContactUsScreen() {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Phone, contentDescription = "Contact Us")
                Text("Contact Us", style = MaterialTheme.typography.headlineSmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "For inquiries or support, please contact us at:",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Email: support@agrilink.com",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Phone: +91 123 456 7890",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}