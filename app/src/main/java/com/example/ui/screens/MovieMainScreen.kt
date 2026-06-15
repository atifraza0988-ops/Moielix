package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.AdminSettings
import com.example.data.model.Movie
import com.example.data.model.Payment
import com.example.data.model.User
import com.example.ui.components.VideoPlayer
import com.example.ui.theme.*
import com.example.ui.viewmodel.MovieViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieMainScreen(
    viewModel: MovieViewModel,
    modifier: Modifier = Modifier
) {
    val movies by viewModel.filteredMovies.collectAsState()
    val allRawMovies by viewModel.rawMovies.collectAsState()
    val payments by viewModel.rawPayments.collectAsState()
    val settings by viewModel.adminSettings.collectAsState()
    val currentUser by viewModel.currentSessionUser.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedGenre by viewModel.selectedGenre.collectAsState()

    val selectedMovie by viewModel.selectedMovie.collectAsState()
    val paymentMovie by viewModel.paymentMovie.collectAsState()
    val activeWatchMovie by viewModel.activeWatchMovie.collectAsState()
    val paymentMessage by viewModel.paymentMessage.collectAsState()

    var currentTab by remember { mutableStateOf("home") } // "home", "space", "admin"

    // Clear payment message after delay
    LaunchedEffect(paymentMessage) {
        if (paymentMessage != null) {
            delay(4000)
            viewModel.paymentMessage.value = null
        }
    }

    if (activeWatchMovie != null) {
        VideoPlayer(
            videoUrl = activeWatchMovie!!.videoUrl,
            title = activeWatchMovie!!.title,
            onClose = { viewModel.activeWatchMovie.value = null }
        )
    } else {
        Scaffold(
            bottomBar = {
                GlassBottomNavigationBar(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it },
                    isAdmin = currentUser?.role == "Admin"
                )
            },
            modifier = modifier
                .fillMaxSize()
                .background(BottleGreenDark)
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .drawBehind {
                        // Ambient radial color splash background for aesthetic lighting
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(BottleGreenLight.copy(alpha = 0.45f), Color.Transparent),
                                center = Offset(size.width * 0.2f, size.height * 0.25f),
                                radius = size.minDimension * 0.7f
                            )
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(CinematicGold.copy(alpha = 0.08f), Color.Transparent),
                                center = Offset(size.width * 0.8f, size.height * 0.75f),
                                radius = size.minDimension * 0.6f
                            )
                        )
                    }
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    GlassNavbar(
                        currentUser = currentUser,
                        onToggleRole = { viewModel.toggleUserAdminRole() },
                        onLogout = { viewModel.logout() }
                    )

                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(250))
                        },
                        label = "tab_navigation"
                    ) { tab ->
                        when (tab) {
                            "home" -> HomeTabView(
                                viewModel = viewModel,
                                movies = movies,
                                allRawMovies = allRawMovies,
                                searchQuery = searchQuery,
                                selectedCategory = selectedCategory,
                                selectedGenre = selectedGenre
                            )
                            "space" -> UserSpaceTabView(
                                viewModel = viewModel,
                                currentUser = currentUser,
                                payments = payments
                            )
                            "admin" -> AdminStudioTabView(
                                viewModel = viewModel,
                                payments = payments,
                                settings = settings ?: AdminSettings()
                            )
                        }
                    }
                }

                // Popup Modals
                selectedMovie?.let { movie ->
                    MovieDetailsModal(
                        movie = movie,
                        isUnlocked = viewModel.isMovieUnlocked(movie),
                        isPending = viewModel.hasPendingPayment(movie),
                        onDismiss = { viewModel.selectedMovie.value = null },
                        onWatchNow = {
                            viewModel.selectedMovie.value = null
                            if (viewModel.isMovieUnlocked(movie)) {
                                viewModel.activeWatchMovie.value = movie
                            } else {
                                viewModel.paymentMovie.value = movie
                            }
                        }
                    )
                }

                paymentMovie?.let { movie ->
                    PaymentPopupDialog(
                        movie = movie,
                        upiId = settings?.upiId ?: "movielix@ybl",
                        upiQrUrl = settings?.upiQrUrl ?: "https://images.unsplash.com/photo-1595079676339-1534801ad6cf?w=400&q=80",
                        onDismiss = { viewModel.paymentMovie.value = null },
                        onSubmitPayment = { txId ->
                            viewModel.submitPayment(movie.id, movie.title, txId)
                        }
                    )
                }

                // Status Toast
                paymentMessage?.let { msg ->
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        action = {
                            TextButton(onClick = { viewModel.paymentMessage.value = null }) {
                                Text("OK", color = CinematicGold)
                            }
                        }
                    ) {
                        Text(text = msg, color = Color.White)
                    }
                }
            }
        }
    }
}

// ---------------- GLASS NAVIGATION & NAVBAR ----------------

@Composable
fun GlassNavbar(
    currentUser: User?,
    onToggleRole: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(GlassBg, RoundedCornerShape(20.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "MOVIELIX",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    fontFamily = FontFamily.SansSerif,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.6f),
                        offset = Offset(1.5f, 1.5f),
                        blurRadius = 3f
                    )
                ),
                color = PrimaryBeige,
                modifier = Modifier.weight(1f)
            )

            if (currentUser != null) {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (currentUser.role == "Admin") AccentRed.copy(alpha = 0.85f) else GlassBg
                        )
                        .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                        .clickable { onToggleRole() }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (currentUser.role == "Admin") "Admin Toggle" else "Standard User",
                        color = PrimaryBeige,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onLogout,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Log out session",
                        tint = CinematicGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GlassBottomNavigationBar(
    currentTab: String,
    onTabSelected: (String) -> Unit,
    isAdmin: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .background(GlassBg, RoundedCornerShape(24.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items = remember(isAdmin) {
                val list = mutableListOf(
                    Triple("home", "Cinema", Icons.Default.PlayArrow),
                    Triple("space", "Tickets", Icons.Default.List)
                )
                if (isAdmin) {
                    list.add(Triple("admin", "Studio", Icons.Default.Settings))
                }
                list
            }

            items.forEach { (tabId, label, icon) ->
                val isSelected = currentTab == tabId
                val transition = updateTransition(targetState = isSelected, label = "tab_pill")
                val pillWidth by transition.animateDp(label = "width") { selected -> if (selected) 90.dp else 46.dp }
                val pillColor by transition.animateColor(label = "color") { selected ->
                    if (selected) BottleGreenLight.copy(alpha = 0.8f) else Color.Transparent
                }

                Box(
                    modifier = Modifier
                        .width(pillWidth)
                        .height(44.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(pillColor)
                        .clickable(
                            onClick = { onTabSelected(tabId) },
                            indication = LocalIndication.current,
                            interactionSource = remember { MutableInteractionSource() }
                        )
                        .testTag("nav_tab_$tabId")
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isSelected) PrimaryBeige else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = label,
                                color = PrimaryBeige,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------- TAB 1: CINEMA HOME VIEW ----------------

@Composable
fun HomeTabView(
    viewModel: MovieViewModel,
    movies: List<Movie>,
    allRawMovies: List<Movie>,
    searchQuery: String,
    selectedCategory: String,
    selectedGenre: String?
) {
    val genresList = listOf("All", "Action", "Sci-Fi", "Drama", "Thriller", "Crime", "Comedy")
    val categoriesList = listOf("All", "Bollywood", "Hollywood", "Web Series", "Trending", "New Releases")

    var autoSpotlightIdx by remember { mutableStateOf(0) }
    val featuredSpotlightMovies = allRawMovies.take(3)

    LaunchedEffect(allRawMovies) {
        if (allRawMovies.isNotEmpty()) {
            while (true) {
                delay(8000)
                autoSpotlightIdx = (autoSpotlightIdx + 1) % featuredSpotlightMovies.size.coerceAtLeast(1)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 76.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Spotlight carousel banner
        if (featuredSpotlightMovies.isNotEmpty()) {
            val currentFeatured = featuredSpotlightMovies.getOrNull(autoSpotlightIdx)
            if (currentFeatured != null) {
                item {
                    CinematicSpotlightHeroBanner(
                        movie = currentFeatured,
                        onWatch = { viewModel.selectedMovie.value = currentFeatured }
                    )
                }
            }
        }

        // Search and Categorization Tab Filters
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search title, genre, synopsis...", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon", tint = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = GlassBg,
                        unfocusedContainerColor = GlassBg,
                        focusedBorderColor = CinematicGold,
                        unfocusedBorderColor = GlassBorder,
                        cursorColor = CinematicGold
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_field")
                )

                // Category Pills Tab list
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categoriesList) { category ->
                        val isSel = selectedCategory == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSel) CinematicGold else GlassBg
                                )
                                .border(1.dp, if (isSel) CinematicGold else GlassBorder, RoundedCornerShape(12.dp))
                                .clickable { viewModel.selectedCategory.value = category }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = category,
                                color = if (isSel) BottleGreenDark else PrimaryBeige,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Sub Genre list of pills
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(genresList) { genre ->
                        val isSel = selectedGenre == genre
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSel) BottleGreenLight else Color.Transparent
                                )
                                .border(1.dp, if (isSel) BottleGreenLight else GlassBorder.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .clickable { viewModel.selectedGenre.value = genre }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = genre,
                                color = if (isSel) PrimaryBeige else TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Spotlight Index",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBeige,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${movies.size} Available",
                    color = CinematicGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Render Cards
        if (movies.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Empty list screen logo",
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No movies match filters.",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            val rows = movies.chunked(2)
            items(rows) { rowMovies ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (movie in rowMovies) {
                        Box(modifier = Modifier.weight(1f)) {
                            MovieCard(
                                movie = movie,
                                isUnlocked = viewModel.isMovieUnlocked(movie),
                                onClick = { viewModel.selectedMovie.value = movie }
                            )
                        }
                    }
                    if (rowMovies.size < 2) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ---------------- MOVIE CARD WITH OVERLAYS & Touch 3D Tilt ----------------

@Composable
fun MovieCard(
    movie: Movie,
    isUnlocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val animatedTiltY by animateFloatAsState(
        targetValue = if (isPressed) 10f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .graphicsLayer {
                // Project visual 3D coordinate tilt on hover/press
                rotationY = animatedTiltY
                scaleX = animatedScale
                scaleY = animatedScale
                cameraDistance = 8f * density
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            }
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(0.75f)) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(movie.posterUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "${movie.title} Poster image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Vignette backdrop
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                            startY = 140f
                        )
                    )
            )

            // Star layout badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating star symbol",
                        tint = CinematicGold,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = movie.rating.toString(),
                        color = PrimaryBeige,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Price/Access badge indicator
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .background(
                        if (isUnlocked) BottleGreenLight.copy(alpha = 0.9f) else AccentRed.copy(alpha = 0.9f),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isUnlocked) Icons.Default.Check else Icons.Default.Lock,
                        contentDescription = "Unlocks status logo",
                        tint = PrimaryBeige,
                        modifier = Modifier.size(9.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = if (isUnlocked) "PLAY" else "₹${movie.price}",
                        color = PrimaryBeige,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Title block
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp)
            ) {
                Text(
                    text = movie.title,
                    color = PrimaryBeige,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${movie.category} • ${movie.genre.substringBefore(",")}",
                    color = TextMuted,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CinematicSpotlightHeroBanner(
    movie: Movie,
    onWatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(movie.posterUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Spotlight highlight poster backdrop",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(0.65f)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, BottleGreenDark.copy(alpha = 0.9f))
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .fillMaxWidth(0.85f)
        ) {
            Box(
                modifier = Modifier
                    .background(CinematicGold, RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "CINEMATIC BLOCKBUSTER",
                    color = BottleGreenDark,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = movie.title,
                color = PrimaryBeige,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = movie.description,
                color = TextMuted,
                fontSize = 10.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = onWatch,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBeige),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Trigger preview dialog",
                        tint = BottleGreenDark,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Show Details",
                        color = BottleGreenDark,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ---------------- TAB 2: USER PROFILE & TICKETS VIEW ----------------

@Composable
fun UserSpaceTabView(
    viewModel: MovieViewModel,
    currentUser: User?,
    payments: List<Payment>
) {
    var emailInput by remember { mutableStateOf("") }
    var showRegPopup by remember { mutableStateOf(false) }
    var regNameInput by remember { mutableStateOf("") }
    var regEmailInput by remember { mutableStateOf("") }

    val userUnlockedMovies = viewModel.rawMovies.collectAsState().value.filter { viewModel.isMovieUnlocked(it) }
    val userPaidTransactions = payments.filter { it.userEmail.equals(currentUser?.email, ignoreCase = true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 76.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (currentUser == null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp)
                        .background(GlassBg, RoundedCornerShape(20.dp))
                        .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBox,
                            contentDescription = "Profile placeholder icon",
                            tint = CinematicGold,
                            modifier = Modifier.size(48.dp)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Access MOVIELIX Premium Space",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBeige
                            )
                            Text(
                                text = "Log in or register to unlock purchased stream coordinates",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                        }

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            placeholder = { Text("Enter your account email...", color = TextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = BottleGreenDark.copy(alpha = 0.5f),
                                unfocusedContainerColor = BottleGreenDark.copy(alpha = 0.5f),
                                focusedBorderColor = CinematicGold,
                                unfocusedBorderColor = GlassBorder,
                                cursorColor = CinematicGold
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("auth_email")
                        )

                        Button(
                            onClick = {
                                if (emailInput.isNotBlank()) {
                                    viewModel.loginUser(emailInput)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CinematicGold),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("LOGIN / JOIN MOVIELIX", color = BottleGreenDark, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = { showRegPopup = true }) {
                                Text("New Cinephile? Register Now", color = PrimaryBeige, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GlassBg, RoundedCornerShape(20.dp))
                        .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = "Active account face",
                                tint = CinematicGold,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = currentUser.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBeige
                                )
                                Text(
                                    text = "${currentUser.email} • Assigned: ${currentUser.role}",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Divider(color = GlassBorder.copy(alpha = 0.3f))

                        Text(
                            text = "Admin Switch Card",
                            color = CinematicGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap the top toggle in navbar anytime to enable Admin Role and approve/verify submitted transactions instantly!",
                            color = TextMuted,
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                    }
                }
            }

            item {
                Text(
                    text = "My Unlocked Coordinates (${userUnlockedMovies.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBeige
                )
            }

            if (userUnlockedMovies.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                            .background(GlassBg, RoundedCornerShape(12.dp))
                            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "All of your purchased coordinate streams will load here.\nPay ₹50 on any movie poster to unlock streaming rights.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp
                        )
                    }
                }
            } else {
                items(userUnlockedMovies) { movie ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GlassBg, RoundedCornerShape(14.dp))
                            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { viewModel.selectedMovie.value = movie }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = movie.posterUrl,
                            contentDescription = movie.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = movie.title, color = PrimaryBeige, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = "${movie.category} • ${movie.duration}", color = TextMuted, fontSize = 10.sp)
                        }

                        Button(
                            onClick = { viewModel.activeWatchMovie.value = movie },
                            colors = ButtonDefaults.buttonColors(containerColor = BottleGreenLight),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Stream logo", tint = PrimaryBeige, modifier = Modifier.size(12.dp))
                                Text("STREAM", color = PrimaryBeige, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "My Transaction Payout Statuses (${userPaidTransactions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBeige
                )
            }

            if (userPaidTransactions.isEmpty()) {
                item {
                    Text(text = "No recorded transaction payloads.", color = TextMuted, fontSize = 10.sp)
                }
            } else {
                items(userPaidTransactions) { pay ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GlassBg, RoundedCornerShape(12.dp))
                            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = pay.movieTitle, color = PrimaryBeige, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(text = "ID: ${pay.transactionId}", color = TextMuted, fontSize = 10.sp)
                            }
                            val badgeColor = when (pay.status) {
                                "Approved" -> Color(0xFF4CAF50)
                                "Rejected" -> AccentRed
                                else -> CinematicGold
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(badgeColor.copy(alpha = 0.15f))
                                    .border(1.dp, badgeColor, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = pay.status.uppercase(),
                                    color = badgeColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRegPopup) {
        Dialog(onDismissRequest = { showRegPopup = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark, RoundedCornerShape(20.dp))
                    .border(1.5.dp, CinematicGold, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "New Cinephile Onboarding",
                        color = PrimaryBeige,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = regNameInput,
                        onValueChange = { regNameInput = it },
                        placeholder = { Text("Select Screen Name...", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CinematicGold,
                            unfocusedBorderColor = GlassBorder,
                            cursorColor = CinematicGold
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = regEmailInput,
                        onValueChange = { regEmailInput = it },
                        placeholder = { Text("Account Login Email...", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CinematicGold,
                            unfocusedBorderColor = GlassBorder,
                            cursorColor = CinematicGold
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showRegPopup = false }) {
                            Text("CANCEL", color = TextMuted)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = {
                                if (regEmailInput.isNotBlank() && regNameInput.isNotBlank()) {
                                    viewModel.registerNewUserByAdmin(regEmailInput, regNameInput, "User")
                                    viewModel.loginUser(regEmailInput)
                                    showRegPopup = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CinematicGold)
                        ) {
                            Text("JOIN PROFILE", color = BottleGreenDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ---------------- TAB 3: ADMIN STUDIO VIEW ----------------

@Composable
fun AdminStudioTabView(
    viewModel: MovieViewModel,
    payments: List<Payment>,
    settings: AdminSettings
) {
    var panelSelection by remember { mutableStateOf("movies") } // "movies", "payments", "settings"

    var editingMovie by remember { mutableStateOf<Movie?>(null) }
    var titleInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }
    var genreInput by remember { mutableStateOf("") }
    var durationInput by remember { mutableStateOf("") }
    var ratingInput by remember { mutableStateOf(4.5f) }
    var categoryInput by remember { mutableStateOf("Bollywood") }
    var posterUrlInput by remember { mutableStateOf("") }
    var videoUrlInput by remember { mutableStateOf("") }

    var settingsUpiId by remember { mutableStateOf(settings.upiId) }
    var settingsQrUrl by remember { mutableStateOf(settings.upiQrUrl) }

    LaunchedEffect(settings) {
        settingsUpiId = settings.upiId
        settingsQrUrl = settings.upiQrUrl
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 76.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GlassBg, RoundedCornerShape(12.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf(
                    "movies" to "Edit Catalog",
                    "payments" to "Verify Txns",
                    "settings" to "Settings"
                ).forEach { (id, label) ->
                    val isSel = panelSelection == id
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) BottleGreenLight else Color.Transparent)
                            .clickable { panelSelection = id }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSel) PrimaryBeige else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        when (panelSelection) {
            "movies" -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GlassBg, RoundedCornerShape(20.dp))
                            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = if (editingMovie != null) "Edit Streaming Node" else "Push Cinematic Node",
                                color = CinematicGold,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )

                            OutlinedTextField(
                                value = titleInput,
                                onValueChange = { titleInput = it },
                                placeholder = { Text("Movie Title...", color = TextMuted) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = CinematicGold,
                                    unfocusedBorderColor = GlassBorder,
                                    cursorColor = CinematicGold
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = descInput,
                                onValueChange = { descInput = it },
                                placeholder = { Text("SynopsisPlot Summary...", color = TextMuted) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = CinematicGold,
                                    unfocusedBorderColor = GlassBorder,
                                    cursorColor = CinematicGold
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = genreInput,
                                    onValueChange = { genreInput = it },
                                    placeholder = { Text("Genre...", color = TextMuted) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = CinematicGold,
                                        unfocusedBorderColor = GlassBorder,
                                        cursorColor = CinematicGold
                                    ),
                                    modifier = Modifier.weight(1.0f)
                                )

                                OutlinedTextField(
                                    value = durationInput,
                                    onValueChange = { durationInput = it },
                                    placeholder = { Text("Duration...", color = TextMuted) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = CinematicGold,
                                        unfocusedBorderColor = GlassBorder,
                                        cursorColor = CinematicGold
                                    ),
                                    modifier = Modifier.weight(1.0f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Rating: ${ratingInput}", color = PrimaryBeige, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                Slider(
                                    value = ratingInput,
                                    onValueChange = { ratingInput = (it * 10f).roundToInt() / 10f },
                                    valueRange = 0f..5.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = CinematicGold,
                                        activeTrackColor = CinematicGold
                                    ),
                                    modifier = Modifier.fillMaxWidth(0.7f)
                                )
                            }

                            Box {
                                var expanded by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(BottleGreenDark.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                        .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                                        .clickable { expanded = true }
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Category: $categoryInput", color = PrimaryBeige, fontSize = 12.sp)
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown indicators", tint = PrimaryBeige)
                                    }
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.background(SurfaceDark)
                                ) {
                                    listOf("Bollywood", "Hollywood", "Web Series").forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat, color = PrimaryBeige) },
                                            onClick = {
                                                categoryInput = cat
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = posterUrlInput,
                                onValueChange = { posterUrlInput = it },
                                placeholder = { Text("Poster URL (Unsplash)...", color = TextMuted) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = CinematicGold,
                                    unfocusedBorderColor = GlassBorder,
                                    cursorColor = CinematicGold
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = videoUrlInput,
                                onValueChange = { videoUrlInput = it },
                                placeholder = { Text("Streaming MP4 URL...", color = TextMuted) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = CinematicGold,
                                    unfocusedBorderColor = GlassBorder,
                                    cursorColor = CinematicGold
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                if (editingMovie != null) {
                                    TextButton(onClick = {
                                        editingMovie = null
                                        titleInput = ""
                                        descInput = ""
                                        genreInput = ""
                                        durationInput = ""
                                        posterUrlInput = ""
                                        videoUrlInput = ""
                                    }) {
                                        Text("CANCEL", color = AccentRed)
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = {
                                        if (titleInput.isNotBlank()) {
                                            val movieObj = Movie(
                                                id = editingMovie?.id ?: 0,
                                                title = titleInput,
                                                description = descInput.ifBlank { "Experience premium high-definition streaming coordinates on MOVIELIX." },
                                                genre = genreInput.ifBlank { "Cinematic drama" },
                                                duration = durationInput.ifBlank { "2h 10m" },
                                                rating = ratingInput,
                                                category = categoryInput,
                                                posterUrl = posterUrlInput.ifBlank { "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=500&q=80" },
                                                videoUrl = videoUrlInput.ifBlank { "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4" },
                                                price = 50,
                                                isPremium = true
                                            )
                                            if (editingMovie != null) {
                                                viewModel.editMovie(movieObj)
                                                editingMovie = null
                                            } else {
                                                viewModel.addMovie(movieObj)
                                            }
                                            titleInput = ""
                                            descInput = ""
                                            genreInput = ""
                                            durationInput = ""
                                            posterUrlInput = ""
                                            videoUrlInput = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CinematicGold)
                                ) {
                                    Text(
                                        text = if (editingMovie != null) "SAVE EDIT" else "ADD MOVIE",
                                        color = BottleGreenDark,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Movielix Index",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBeige
                    )
                }

                val allMoviesList = viewModel.rawMovies.value
                if (allMoviesList.isEmpty()) {
                    item {
                        Text(text = "Movielix index empty.", color = TextMuted)
                    }
                } else {
                    items(allMoviesList) { m ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GlassBg, RoundedCornerShape(12.dp))
                                .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = m.posterUrl,
                                contentDescription = m.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = m.title, color = PrimaryBeige, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(text = "${m.category} • ${m.genre}", color = TextMuted, fontSize = 10.sp)
                            }

                            IconButton(onClick = {
                                editingMovie = m
                                titleInput = m.title
                                descInput = m.description
                                genreInput = m.genre
                                durationInput = m.duration
                                ratingInput = m.rating
                                categoryInput = m.category
                                posterUrlInput = m.posterUrl
                                videoUrlInput = m.videoUrl
                            }) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit details log", tint = CinematicGold, modifier = Modifier.size(16.dp))
                            }

                            IconButton(onClick = { viewModel.deleteMovie(m) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete coordinates", tint = AccentRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            "payments" -> {
                val pendingPayments = payments.filter { it.status == "Pending" }
                val approvedPayments = payments.filter { it.status != "Pending" }

                item {
                    Text(
                        text = "Awaiting verification (${pendingPayments.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBeige
                    )
                }

                if (pendingPayments.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No pending PhonePe payouts for approval.", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                } else {
                    items(pendingPayments) { pay ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GlassBg, RoundedCornerShape(16.dp))
                                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = pay.movieTitle, color = PrimaryBeige, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(text = "Buyer: ${pay.userEmail}", color = TextMuted, fontSize = 10.sp)
                                    }
                                    Text(text = "₹${pay.amount}", color = CinematicGold, fontWeight = FontWeight.Bold)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "TXN: ${pay.transactionId}",
                                        color = CinematicGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Button(
                                            onClick = { viewModel.rejectPayment(pay) },
                                            colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                                            contentPadding = PaddingValues(horizontal = 8.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Text("Reject", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = { viewModel.approvePayment(pay) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                            contentPadding = PaddingValues(horizontal = 8.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Text("Approve", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Payout Logs History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBeige
                    )
                }

                if (approvedPayments.isEmpty()) {
                    item {
                        Text("No logged transactions processed yet.", color = TextMuted, fontSize = 11.sp)
                    }
                } else {
                    items(approvedPayments) { pay ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GlassBg, RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = pay.movieTitle, color = PrimaryBeige, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(text = "${pay.userEmail} • TXN: ${pay.transactionId}", color = TextMuted, fontSize = 9.sp)
                            }
                            Text(
                                text = pay.status.uppercase(),
                                color = if (pay.status == "Approved") Color(0xFF4CAF50) else AccentRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            "settings" -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GlassBg, RoundedCornerShape(16.dp))
                            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Admin PhonePe Gateway Gateway",
                                color = CinematicGold,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )

                            OutlinedTextField(
                                value = settingsUpiId,
                                onValueChange = { settingsUpiId = it },
                                placeholder = { Text("PhonePe UPI Address...", color = TextMuted) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = CinematicGold,
                                    unfocusedBorderColor = GlassBorder,
                                    cursorColor = CinematicGold
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = settingsQrUrl,
                                onValueChange = { settingsQrUrl = it },
                                placeholder = { Text("UPI QR Code Image URL...", color = TextMuted) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = CinematicGold,
                                    unfocusedBorderColor = GlassBorder,
                                    cursorColor = CinematicGold
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    viewModel.updateSettings(settingsUpiId, settingsQrUrl)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CinematicGold),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("UPDATE SYSTEM GATEWAY", color = BottleGreenDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- DIALOG 1: DETAILED MOVIE MODAL ----------------

@Composable
fun MovieDetailsModal(
    movie: Movie,
    isUnlocked: Boolean,
    isPending: Boolean,
    onDismiss: () -> Unit,
    onWatchNow: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark, RoundedCornerShape(24.dp))
                .border(1.5.dp, BottleGreenLight, RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    AsyncImage(
                        model = movie.posterUrl,
                        contentDescription = "Details backdrop image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, SurfaceDark)
                                )
                            )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1.3f)) {
                        Text(
                            text = movie.title,
                            color = PrimaryBeige,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        labelValue(label = "Category: ", value = movie.category)
                        labelValue(label = "Genre: ", value = movie.genre)
                        labelValue(label = "Length: ", value = movie.duration)
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.weight(0.7f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Detail star",
                                tint = CinematicGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = movie.rating.toString(),
                                color = PrimaryBeige,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Access: ₹${movie.price}",
                            color = CinematicGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Divider(color = GlassBorder.copy(alpha = 0.3f))

                Text(
                    text = movie.description,
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("BACK TO LIST", color = TextMuted, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }

                    Button(
                        onClick = onWatchNow,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isUnlocked) BottleGreenLight else CinematicGold
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("action_watch_now")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isUnlocked) Icons.Default.PlayArrow else Icons.Default.ShoppingCart,
                                contentDescription = "Watch button logic emblem",
                                tint = if (isUnlocked) PrimaryBeige else BottleGreenDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isUnlocked) "PLAY STREAM" 
                                       else if (isPending) "VERIFYING PAY"
                                       else "WATCH (₹${movie.price})",
                                color = if (isUnlocked) PrimaryBeige else BottleGreenDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun labelValue(label: String, value: String) {
    Row {
        Text(text = label, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(text = value, color = PrimaryBeige, fontSize = 10.sp)
    }
}

// ---------------- DIALOG 2: UPI PAYMENT GATEWAY POPUP ----------------

@Composable
fun PaymentPopupDialog(
    movie: Movie,
    upiId: String,
    upiQrUrl: String,
    onDismiss: () -> Unit,
    onSubmitPayment: (String) -> Unit
) {
    var txtIdInput by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark, RoundedCornerShape(24.dp))
                .border(2.dp, CinematicGold, RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "MOVIELIX SECURE UPI",
                    color = CinematicGold,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "Unlock coordinates for: \n${movie.title}",
                    color = PrimaryBeige,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp
                )

                Divider(color = GlassBorder.copy(alpha = 0.3f))

                Box(
                    modifier = Modifier
                        .background(BottleGreenDark, RoundedCornerShape(10.dp))
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "AMOUNT DUED: ₹${movie.price}",
                        color = CinematicGold,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = upiQrUrl,
                        contentDescription = "UPI scanning QR graphic",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Text(
                    text = "Scan QR with PhonePe or pay to UPI ID:\n$upiId",
                    fontSize = 10.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp
                )

                OutlinedTextField(
                    value = txtIdInput,
                    onValueChange = { 
                        txtIdInput = it 
                        inputError = false
                    },
                    placeholder = { Text("PhonePe Transaction ID...", color = TextMuted) },
                    singleLine = true,
                    isError = inputError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = GlassBg,
                        unfocusedContainerColor = GlassBg,
                        focusedBorderColor = CinematicGold,
                        unfocusedBorderColor = GlassBorder,
                        cursorColor = CinematicGold,
                        errorBorderColor = AccentRed
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_txn_input")
                )
                if (inputError) {
                    Text("Transaction ID is required to process unlock actions.", color = AccentRed, fontSize = 9.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { onDismiss() }) {
                        Text("CANCEL PAY", color = TextMuted, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }

                    Button(
                        onClick = {
                            if (txtIdInput.isBlank()) {
                                inputError = true
                            } else {
                                onSubmitPayment(txtIdInput)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CinematicGold),
                        modifier = Modifier.testTag("submit_payment_txn")
                    ) {
                        Text("SUBMIT ID", color = BottleGreenDark, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
