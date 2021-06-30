package com.example.good2go

data class Post (
    var id: Int = 0,
    val title: String,
    val description: String,
    val imageUri: String,
    val latitude: Double,
    val longitude: Double
)