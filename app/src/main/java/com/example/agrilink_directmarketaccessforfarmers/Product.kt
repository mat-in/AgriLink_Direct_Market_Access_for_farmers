package com.example.agrilink_directmarketaccessforfarmers

data class Product(
    val name: String = "",
    val price: String = "",
    val images: List<String> = emptyList(),
    val userId: String = "" // Added userId field
)