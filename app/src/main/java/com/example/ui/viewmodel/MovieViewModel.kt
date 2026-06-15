package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AdminSettings
import com.example.data.model.Movie
import com.example.data.model.Payment
import com.example.data.model.User
import com.example.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MovieViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MovieRepository

    // Current Session User
    val currentSessionUser = MutableStateFlow<User?>(null)

    // Raw Flows from Database
    val rawMovies: StateFlow<List<Movie>>
    val rawPayments: StateFlow<List<Payment>>
    val adminSettings: StateFlow<AdminSettings?>

    // Search and Genre filters
    val searchQuery = MutableStateFlow("")
    val selectedGenre = MutableStateFlow<String?>("All")
    val selectedCategory = MutableStateFlow<String>("All") // "All", "Bollywood", "Hollywood", "Web Series", "Trending", "New Releases"

    // Filtered Movies Flow
    val filteredMovies: StateFlow<List<Movie>>

    // UI Active selection states
    val selectedMovie = MutableStateFlow<Movie?>(null)
    val paymentMovie = MutableStateFlow<Movie?>(null)
    val activeWatchMovie = MutableStateFlow<Movie?>(null)

    // Transaction ID input for payment
    val inputTransactionId = MutableStateFlow("")
    val paymentMessage = MutableStateFlow<String?>(null)
    val unlockedMovies = MutableStateFlow<Set<Int>>(emptySet())

    init {
        val database = AppDatabase.getDatabase(application)
        repository = MovieRepository(
            database.movieDao(),
            database.paymentDao(),
            database.userDao(),
            database.adminSettingsDao()
        )

        rawMovies = repository.allMovies.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        rawPayments = repository.allPayments.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        adminSettings = repository.adminSettings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        // Combine search, category, and genre with movies to produce filtered movies
        filteredMovies = combine(
            rawMovies,
            searchQuery,
            selectedGenre,
            selectedCategory
        ) { moviesList, query, genre, category ->
            moviesList.filter { movie ->
                val matchesQuery = movie.title.contains(query, ignoreCase = true) ||
                        movie.genre.contains(query, ignoreCase = true) ||
                        movie.description.contains(query, ignoreCase = true)

                val matchesGenre = if (genre == null || genre == "All") {
                    true
                } else {
                    movie.genre.contains(genre, ignoreCase = true)
                }

                val matchesCategory = if (category == "All") {
                    true
                } else if (category == "Trending") {
                    // Say rating > 4.5 is trending
                    movie.rating >= 4.6f
                } else if (category == "New Releases") {
                    // Custom mapping for demonstration
                    movie.id % 2 != 0
                } else {
                    movie.category.equals(category, ignoreCase = true)
                }

                matchesQuery && matchesGenre && matchesCategory
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Run db Seed and set initial user status
        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
            // Set default login as standard User
            loginUser("user@movielix.com")
            
            // Sync unlocked movies initially based on existing approved payments
            syncUnlockedMovies()
        }
    }

    private suspend fun syncUnlockedMovies() {
        val email = currentSessionUser.value?.email ?: return
        val approvedPayments = rawPayments.value
            .filter { it.userEmail.equals(email, ignoreCase = true) && it.status == "Approved" }
            .map { it.movieId }
            .toSet()
        unlockedMovies.value = approvedPayments
    }

    // Authenticate/Login user
    fun loginUser(email: String) {
        viewModelScope.launch {
            val trimmedEmail = email.trim()
            val existingUser = repository.getUserByEmail(trimmedEmail).firstOrNull()
            if (existingUser != null) {
                currentSessionUser.value = existingUser
            } else {
                // If user doesn't exist, register on the fly!
                val role = if (trimmedEmail.contains("admin", ignoreCase = true)) "Admin" else "User"
                val newUser = User(
                    email = trimmedEmail,
                    name = trimmedEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                    role = role
                )
                repository.insertUser(newUser)
                currentSessionUser.value = newUser
            }
            syncUnlockedMovies()
        }
    }

    fun logout() {
        currentSessionUser.value = null
        unlockedMovies.value = emptySet()
        activeWatchMovie.value = null
    }

    // Switch between user and admin easily for testing!
    fun toggleUserAdminRole() {
        val currentUser = currentSessionUser.value ?: return
        val newRole = if (currentUser.role == "Admin") "User" else "Admin"
        viewModelScope.launch {
            val updatedUser = currentUser.copy(role = newRole)
            repository.updateUser(updatedUser)
            currentSessionUser.value = updatedUser
            syncUnlockedMovies()
        }
    }

    // Admin Add Movie
    fun addMovie(movie: Movie) {
        viewModelScope.launch {
            repository.insertMovie(movie)
        }
    }

    // Admin Edit Movie
    fun editMovie(movie: Movie) {
        viewModelScope.launch {
            repository.updateMovie(movie)
        }
    }

    // Admin Delete Movie
    fun deleteMovie(movie: Movie) {
        viewModelScope.launch {
            repository.deleteMovie(movie)
        }
    }

    // Submit Payment
    fun submitPayment(movieId: Int, movieTitle: String, txId: String) {
        val user = currentSessionUser.value
        if (user == null) {
            paymentMessage.value = "Please log in to make a payment."
            return
        }
        if (txId.isBlank()) {
            paymentMessage.value = "Transaction ID cannot be blank."
            return
        }
        viewModelScope.launch {
            val payment = Payment(
                movieId = movieId,
                movieTitle = movieTitle,
                userEmail = user.email,
                transactionId = txId,
                amount = 50,
                status = "Pending"
            )
            repository.insertPayment(payment)
            paymentMessage.value = "Payment Submitted! Awaiting Admin Approval."
            // Close payment modal
            paymentMovie.value = null
        }
    }

    // Admin approve payment and unlock streaming
    fun approvePayment(payment: Payment) {
        viewModelScope.launch {
            val updatedPayment = payment.copy(status = "Approved")
            repository.updatePayment(updatedPayment)
            syncUnlockedMovies()
        }
    }

    // Admin reject payment
    fun rejectPayment(payment: Payment) {
        viewModelScope.launch {
            val updatedPayment = payment.copy(status = "Rejected")
            repository.updatePayment(updatedPayment)
            syncUnlockedMovies()
        }
    }

    // Check if current user has streaming access to a movie
    fun isMovieUnlocked(movie: Movie): Boolean {
        if (!movie.isPremium) return true
        val user = currentSessionUser.value ?: return false
        if (user.role == "Admin") return true // Admin unlocks all
        return unlockedMovies.value.contains(movie.id)
    }

    // Check if user has already submitted a payment that is pending
    fun hasPendingPayment(movie: Movie): Boolean {
        val user = currentSessionUser.value ?: return false
        return rawPayments.value.any { 
            it.movieId == movie.id && 
            it.userEmail.equals(user.email, ignoreCase = true) && 
            it.status == "Pending" 
        }
    }

    // Update settings
    fun updateSettings(upiId: String, qrUrl: String) {
        viewModelScope.launch {
            val current = adminSettings.value ?: AdminSettings()
            repository.updateAdminSettings(current.copy(upiId = upiId, upiQrUrl = qrUrl))
        }
    }

    // Register User direct
    fun registerNewUserByAdmin(email: String, name: String, role: String) {
        viewModelScope.launch {
            val newUser = User(email = email.trim(), name = name, role = role)
            repository.insertUser(newUser)
        }
    }

    // Toggle Block User
    fun toggleBlockUser(user: User) {
        viewModelScope.launch {
            repository.updateUser(user.copy(isBlocked = !user.isBlocked))
        }
    }
}
