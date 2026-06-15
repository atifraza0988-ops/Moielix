package com.example.data.repository

import com.example.data.local.AdminSettingsDao
import com.example.data.local.MovieDao
import com.example.data.local.PaymentDao
import com.example.data.local.UserDao
import com.example.data.model.AdminSettings
import com.example.data.model.Movie
import com.example.data.model.Payment
import com.example.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class MovieRepository(
    private val movieDao: MovieDao,
    private val paymentDao: PaymentDao,
    private val userDao: UserDao,
    private val adminSettingsDao: AdminSettingsDao
) {
    // Movies Flow
    val allMovies: Flow<List<Movie>> = movieDao.getAllMovies()
    
    fun getMovieById(id: Int): Flow<Movie?> = movieDao.getMovieById(id)

    suspend fun insertMovie(movie: Movie): Long = movieDao.insertMovie(movie)

    suspend fun updateMovie(movie: Movie) = movieDao.updateMovie(movie)

    suspend fun deleteMovie(movie: Movie) = movieDao.deleteMovie(movie)

    suspend fun deleteMovieById(id: Int) = movieDao.deleteMovieById(id)

    // Payments Flow
    val allPayments: Flow<List<Payment>> = paymentDao.getAllPayments()

    fun getPaymentsByUser(email: String): Flow<List<Payment>> = paymentDao.getPaymentsByUser(email)

    fun getPaymentState(movieId: Int, email: String): Flow<Payment?> = paymentDao.getPaymentState(movieId, email)

    suspend fun insertPayment(payment: Payment): Long = paymentDao.insertPayment(payment)

    suspend fun updatePayment(payment: Payment) = paymentDao.updatePayment(payment)

    suspend fun deletePayment(payment: Payment) = paymentDao.deletePayment(payment)

    // Users Flow
    val allUsers: Flow<List<User>> = userDao.getAllUsers()

    fun getUserByEmail(email: String): Flow<User?> = userDao.getUserByEmail(email)

    suspend fun insertUser(user: User) = userDao.insertUser(user)

    suspend fun updateUser(user: User) = userDao.updateUser(user)

    suspend fun deleteUser(user: User) = userDao.deleteUser(user)

    // Admin Settings Flow
    val adminSettings: Flow<AdminSettings?> = adminSettingsDao.getSettings()

    suspend fun updateAdminSettings(settings: AdminSettings) {
        adminSettingsDao.insertOrUpdateSettings(settings)
    }

    // Helper to run seed operations safely if database is empty
    suspend fun seedDatabaseIfEmpty() {
        // Seed Settings if null
        val currentSettings = adminSettings.firstOrNull()
        if (currentSettings == null) {
            adminSettingsDao.insertOrUpdateSettings(AdminSettings())
        }

        // Seed Users if none exist
        val users = userDao.getAllUsers().firstOrNull()
        if (users.isNullOrEmpty()) {
            userDao.insertUser(User(email = "admin@movielix.com", name = "Chief Administrator", role = "Admin"))
            userDao.insertUser(User(email = "user@movielix.com", name = "Premium Cinephile", role = "User"))
        }

        // Seed Movies if none exist
        val movies = movieDao.getAllMovies().firstOrNull()
        if (movies.isNullOrEmpty()) {
            val defaultMovies = listOf(
                Movie(
                    title = "Jawan",
                    description = "An emotional journey of a man who is set to rectify the wrongs in the society. Out to fight evil with all his power, his high-action stunts are mindbloat.",
                    genre = "Action, Thriller",
                    duration = "2h 49m",
                    rating = 4.8f,
                    category = "Bollywood",
                    posterUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=500&q=80",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                ),
                Movie(
                    title = "Interstellar",
                    description = "When Earth becomes uninhabitable, a team of explorers travels through a wormhole in space in an attempt to ensure humanity's survival.",
                    genre = "Sci-Fi, Adventure",
                    duration = "2h 49m",
                    rating = 4.9f,
                    category = "Hollywood",
                    posterUrl = "https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=500&q=80",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                    trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
                ),
                Movie(
                    title = "Animal",
                    description = "A son's love for his father. Often, his father is busy with work, breeding resentment and deep obsession that spirals out of control in of family war.",
                    genre = "Action, Crime",
                    duration = "3h 21m",
                    rating = 4.3f,
                    category = "Bollywood",
                    posterUrl = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=500&q=80",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
                ),
                Movie(
                    title = "Dune: Part Two",
                    description = "Paul Atreides unites with Chani and the Fremen while seeking revenge against the conspirators who destroyed his family.",
                    genre = "Sci-Fi, Epic",
                    duration = "2h 46m",
                    rating = 4.8f,
                    category = "Hollywood",
                    posterUrl = "https://images.unsplash.com/photo-1478720143022-385f704d3b73?w=500&q=80",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                    trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"
                ),
                Movie(
                    title = "Stranger Things",
                    description = "When a young boy vanishes, a small town uncovers a mystery involving secret experiments, terrifying supernatural forces and one strange little girl.",
                    genre = "Sci-Fi, Horror",
                    duration = "55m (Ep)",
                    rating = 4.8f,
                    category = "Web Series",
                    posterUrl = "https://images.unsplash.com/photo-1574267431622-790184433bec?w=500&q=80",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                    trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4"
                ),
                Movie(
                    title = "Dunki",
                    description = "Four friends from a village in Punjab share a common dream: to go to England. Their problem is that they have neither the visa nor the ticket.",
                    genre = "Drama, Comedy",
                    duration = "2h 40m",
                    rating = 4.4f,
                    category = "Bollywood",
                    posterUrl = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=500&q=80",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
                    trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4"
                ),
                Movie(
                    title = "The Dark Knight",
                    description = "When the menace known as the Joker wreaks havoc and chaos on the people of Gotham, Batman must accept one of the greatest psychological and physical tests.",
                    genre = "Action, Thriller",
                    duration = "2h 32m",
                    rating = 4.9f,
                    category = "Hollywood",
                    posterUrl = "https://images.unsplash.com/photo-1598899134739-24c46f58b8c0?w=500&q=80",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                    trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
                )
            )
            for (movie in defaultMovies) {
                movieDao.insertMovie(movie)
            }
        }
    }
}
