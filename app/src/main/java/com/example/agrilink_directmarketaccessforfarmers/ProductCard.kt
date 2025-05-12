package com.example.agrilink_directmarketaccessforfarmers

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun <T> ProductCard(product: T) {
    var imageBitmaps by remember { mutableStateOf<List<android.graphics.Bitmap>>(emptyList()) }

    LaunchedEffect(if (product is Product) product.images else (product as ProductListing).images) {
        val images = if (product is Product) product.images else (product as ProductListing).images
        val bitmaps = images.mapNotNull { base64String ->
            try {
                val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (e: Exception) {
                Log.e("Base64", "Error decoding Base64 image", e)
                null
            }
        }
        imageBitmaps = bitmaps
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Product: ${if (product is Product) product.name else (product as ProductListing).name}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Price: ${if (product is Product) product.price else (product as ProductListing).price}", style = MaterialTheme.typography.bodyMedium)

            Row {
                imageBitmaps.forEach { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Product Image",
                        modifier = Modifier
                            .size(100.dp)
                            .padding(4.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}