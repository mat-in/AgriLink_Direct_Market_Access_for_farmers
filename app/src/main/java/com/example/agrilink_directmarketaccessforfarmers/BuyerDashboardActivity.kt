package com.example.agrilink_directmarketaccessforfarmers

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.agrilink_directmarketaccessforfarmers.ui.theme.AgriLinkDirectMarketAccessForFarmersTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BuyerDashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgriLinkDirectMarketAccessForFarmersTheme {
                BuyerDashboardScreen()
            }
        }
    }
}

@Composable
fun BuyerDashboardScreen() {
    var selectedScreen by remember { mutableStateOf(0) }
    val screens = listOf("Feed", "Profile", "Settings")
    val icons = listOf(
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
                0 -> BuyerFeedScreen()
                1 -> ProfileScreen() // Placeholder for buyer profile.
                2 -> SettingsScreen() // Placeholder for buyer settings.
            }
        }
    }
}

data class ProductListing( // Renamed data class
    val name: String = "",
    val price: String = "",
    val images: List<String> = emptyList(),
    val userId: String = ""
)

@Composable
fun BuyerFeedScreen() {
    var products by remember { mutableStateOf<List<Pair<ProductListing, Pair<String, String?>>>>(emptyList()) }
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    LaunchedEffect(Unit) {
        firestore.collection("products")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("Firestore", "Error fetching products", e)
                    return@addSnapshotListener
                }
                val productList = snapshot?.documents?.map { document ->
                    val product = ProductListing(
                        name = document.getString("name") ?: "Unknown",
                        price = document.getString("price") ?: "0",
                        images = document.get("images") as? List<String> ?: emptyList(),
                        userId = document.getString("userId") ?: ""
                    )
                    product
                } ?: emptyList()

                val productUserPair = mutableListOf<Pair<ProductListing, Pair<String,String?>>>()

                productList.forEach { product ->
                    firestore.collection("users").document(product.userId).get()
                        .addOnSuccessListener { userDocument ->
                            val userName = userDocument.getString("name") ?: "unknown"
                            val userPhoto = userDocument.getString("photoUrl")
                            productUserPair.add(Pair(product, Pair(userName, userPhoto)))
                            products = productUserPair.toList()
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firestore", "Error fetching user data", e)
                        }
                }
            }
    }

    Column {
        Text("Buyer Feed - Browse Products", modifier = Modifier.padding(16.dp))
        LazyColumn {
            items(products) { (product, userDetails) ->
                ProductCardWithUserName(
                    product,
                    userDetails.first,
                    userDetails.second,
                    onMessageClick = {
                        val buyerId = auth.currentUser?.uid
                        if (buyerId != null) {
                            val intent = Intent(context, ChatActivity::class.java).apply {
                                putExtra("buyerId", buyerId)
                                putExtra("farmerId", product.userId)
                            }
                            context.startActivity(intent)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun <T> ProductCardWithUserName(
    product: T,
    userName: String,
    photoUrl: String?,
    onMessageClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
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
            Text(
                text = "Product: ${if (product is Product) product.name else (product as ProductListing).name}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Price: ${if (product is Product) product.price else (product as ProductListing).price}",
                style = MaterialTheme.typography.bodyMedium
            )
            ProductCard(product = if (product is Product) product else (product as ProductListing))
        }
    }
}


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
            "Phone: +1 123 456 7890",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
@Composable
fun ProfileScreen() {
    Column {
        Text("Buyer Profile")
    }
}