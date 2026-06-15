package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "movies")
data class Movie(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val genre: String,
    val duration: String,
    val rating: Float,
    val category: String, // "Bollywood", "Hollywood", "Trending", "New Releases", "Web Series"
    val posterUrl: String,
    val videoUrl: String,
    val price: Int = 50,
    val isPremium: Boolean = true,
    val trailerUrl: String = ""
) : Serializable

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val movieId: Int,
    val movieTitle: String,
    val userEmail: String,
    val transactionId: String,
    val amount: Int = 50,
    val status: String = "Pending", // "Pending", "Approved", "Rejected"
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "users")
data class User(
    @PrimaryKey val email: String,
    val name: String,
    val role: String = "User", // "Admin" or "User"
    val isBlocked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "admin_settings")
data class AdminSettings(
    @PrimaryKey val id: Int = 1,
    val upiId: String = "movielix@ybl",
    val upiQrUrl: String = "https://images.unsplash.com/photo-1595079676339-1534801ad6cf?w=400&q=80", // QR-code code representation
    val appCustomText: String = "Enjoy the Premium Experience"
) : Serializable
