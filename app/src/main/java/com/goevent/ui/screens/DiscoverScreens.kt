package com.goevent.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.goevent.ui.i18n.tr
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.goevent.core.model.EventSort
import com.goevent.core.util.Formatters
import com.goevent.ui.components.CosmicTag
import com.goevent.ui.components.EmptyView
import com.goevent.ui.components.ErrorView
import com.goevent.ui.components.EventCard
import com.goevent.ui.components.EventImage
import com.goevent.ui.components.EventRow
import com.goevent.ui.components.FavouriteButton
import com.goevent.ui.components.GalaxyScreen
import com.goevent.ui.components.GlassCard
import com.goevent.ui.components.GalaxyTextField
import com.goevent.ui.components.GradientButton
import com.goevent.ui.components.InfoRow
import com.goevent.ui.components.LoadingView
import com.goevent.ui.components.OutlineButton
import com.goevent.ui.components.SectionHeader
import com.goevent.ui.components.CategoryBubble
import com.goevent.ui.components.eventImageUrl
import com.goevent.ui.components.toast
import com.goevent.ui.galaxy.GalaxyBackground
import com.goevent.ui.galaxy.GalaxyVariant
import com.goevent.ui.galaxy.MoonIllustration
import com.goevent.ui.galaxy.SpiralGalaxy
import com.goevent.ui.theme.Galaxy
import com.goevent.ui.theme.cosmic
import com.goevent.ui.viewmodel.EventDetailsViewModel
import com.goevent.ui.viewmodel.ExploreViewModel
import com.goevent.ui.viewmodel.FavoritesViewModel
import com.goevent.ui.viewmodel.HomeViewModel
import com.goevent.ui.viewmodel.SessionViewModel
import com.goevent.ui.viewmodel.appViewModel
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import java.util.Calendar

@Composable
private fun ShowFavouriteMessages(favorites: FavoritesViewModel) {
    val ctx = LocalContext.current
    val msg by favorites.message.collectAsState()
    LaunchedEffect(msg) { msg?.let { ctx.toast(it); favorites.clearMessage() } }
}

// ===================================================================== HOME

@Composable
fun HomeScreen(
    session: SessionViewModel,
    favorites: FavoritesViewModel,
    onEvent: (String) -> Unit,
    onExplore: (String?) -> Unit,
    onNotifications: () -> Unit,
    onProfile: () -> Unit,
    onRewards: () -> Unit
) {
    val vm = appViewModel { HomeViewModel(it) }
    val s by vm.state.collectAsState()
    val ss by session.state.collectAsState()
    val favIds by favorites.ids.collectAsState()
    val profile = ss.profile
    ShowFavouriteMessages(favorites)
    LaunchedEffect(profile?.id) { vm.load() }
    val categoryNames = remember(s.categories) { s.categories.associate { it.id to it.name } }

    GalaxyScreen(GalaxyVariant.HOME) {
        when {
            s.loading && s.upcoming.isEmpty() -> LoadingView()
            s.error != null && s.upcoming.isEmpty() -> ErrorView(s.error.orEmpty(), vm::load)
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                            Text(Formatters.greeting(hour), color = cosmic.textSecondary, fontSize = 14.sp)
                            Text(
                                Formatters.firstName(profile?.fullName.orEmpty()), style = MaterialTheme.typography.headlineMedium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onNotifications, Modifier.size(48.dp)) {
                            Box {
                                Icon(Icons.Rounded.Notifications, "Notifications", Modifier.size(26.dp))
                                if (s.unread > 0) {
                                    Box(Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp).size(10.dp).clip(CircleShape).background(cosmic.pink))
                                }
                            }
                        }
                        Avatar(profile?.fullName.orEmpty(), profile?.avatarUrl, Modifier.size(44.dp).clickable(onClick = onProfile))
                    }
                }
                item { HeroCard { onExplore(null) } }
                item { PointsCard(profile?.gopointsBalance ?: 0, onRewards) }
                if (s.categories.isNotEmpty()) item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("Categories")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(s.categories, key = { it.id }) { c -> CategoryBubble(c.name, c.iconName, false, { onExplore(c.id) }) }
                        }
                    }
                }
                if (s.featured.isNotEmpty()) item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("Featured events", actionLabel = "See all", onAction = { onExplore(null) })
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(s.featured, key = { it.id }) { e ->
                                EventCard(e, categoryNames[e.categoryId], e.id in favIds, { onEvent(e.id) }, { favorites.toggle(e.id) }, Modifier.width(260.dp), imageHeight = 150.dp)
                            }
                        }
                    }
                }
                item { SectionHeader("Upcoming events") }
                if (s.upcoming.isEmpty()) {
                    item { Text("No upcoming events right now. Check back soon.", color = cosmic.textSecondary) }
                } else {
                    items(s.upcoming, key = { "up-" + it.id }) { e ->
                        EventRow(e, e.id in favIds, { onEvent(e.id) }, { favorites.toggle(e.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun Avatar(name: String, url: String?, modifier: Modifier = Modifier) {
    Box(modifier.clip(CircleShape).background(Galaxy.galaxy), contentAlignment = Alignment.Center) {
        Text(Formatters.initials(name), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        if (!url.isNullOrBlank()) {
            AsyncImage(url, "Profile picture", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}

@Composable
private fun HeroCard(onDiscover: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Box(Modifier.fillMaxWidth().height(270.dp).clip(shape).background(Galaxy.nebula)) {
        SpiralGalaxy(Modifier.size(230.dp).align(Alignment.CenterEnd).offset(x = 60.dp).alpha(0.9f), seed = 5)
        MoonIllustration(Modifier.size(90.dp).align(Alignment.TopEnd).offset(x = (-10).dp, y = 6.dp), crescent = true)
        Column(Modifier.align(Alignment.CenterStart).padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 20.dp).fillMaxWidth(0.66f)) {
            Text(tr("Discover the universe of events"), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp)
            Spacer(Modifier.height(6.dp))
            Text(tr("Concerts, tech, sport, art and more."), color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onDiscover,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF5B21B6)),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(tr("Discover Events"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PointsCard(balance: Int, onRewards: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(24.dp)).background(Galaxy.galaxy).clickable(onClick = onRewards).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Star, null, Modifier.size(32.dp), tint = Color(0xFFFDE68A))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Your GoPoints", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
            Text("$balance", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        Text("View Rewards", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ===================================================================== EXPLORE + SEARCH RESULTS

@Composable
private fun ChipMenu(label: String, active: Boolean, icon: ImageVector, options: List<String>, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = active, onClick = { open = true }, label = { Text(label, maxLines = 1) },
            leadingIcon = { Icon(icon, null, Modifier.size(18.dp)) },
            trailingIcon = { Icon(Icons.Rounded.ExpandMore, null, Modifier.size(18.dp)) }
        )
        DropdownMenu(open, { open = false }) {
            options.forEachIndexed { i, o -> DropdownMenuItem({ Text(o) }, { open = false; onSelect(i) }) }
        }
    }
}

@Composable
fun ExploreScreen(favorites: FavoritesViewModel, initialCategory: String?, onEvent: (String) -> Unit) {
    val vm = appViewModel { ExploreViewModel(it) }
    val s by vm.state.collectAsState()
    val favIds by favorites.ids.collectAsState()
    ShowFavouriteMessages(favorites)
    LaunchedEffect(initialCategory) { if (initialCategory != null) vm.update { it.copy(categoryId = initialCategory) } }
    val q = s.query
    val names = remember(s.categories) { s.categories.associate { it.id to it.name } }
    val dateOptions = listOf("Any", "Today", "This week", "This month")
    var dateLabel by remember { mutableStateOf("Any") }
    val sortOptions = listOf("Soonest" to EventSort.DATE, "Price: low to high" to EventSort.PRICE_LOW, "Price: high to low" to EventSort.PRICE_HIGH)
    val filtered = q.search.isNotBlank() || q.categoryId != null || q.city != null || dateLabel != "Any"

    GalaxyScreen(GalaxyVariant.EXPLORE) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(8.dp))
            Text(if (q.search.isNotBlank()) "Results" else "Explore", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(12.dp))
            GalaxyTextField(
                q.search, { v -> vm.update { it.copy(search = v) } }, "Search events, venues, cities",
                leadingIcon = Icons.Rounded.Search, imeAction = ImeAction.Search
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    ChipMenu(sortOptions.first { it.second == q.sort }.first, q.sort != EventSort.DATE, Icons.Rounded.Sort, sortOptions.map { it.first }) { i ->
                        vm.update { it.copy(sort = sortOptions[i].second) }
                    }
                }
                item {
                    ChipMenu(q.city ?: "All cities", q.city != null, Icons.Rounded.LocationCity, listOf("All cities") + s.cities) { i ->
                        vm.update { it.copy(city = if (i == 0) null else s.cities[i - 1]) }
                    }
                }
                item {
                    ChipMenu(if (dateLabel == "Any") "Any date" else dateLabel, dateLabel != "Any", Icons.Rounded.CalendarMonth, dateOptions) { i ->
                        dateLabel = dateOptions[i]
                        val (from, to) = Formatters.dateRange(dateLabel)
                        vm.update { it.copy(dateFrom = from, dateTo = to) }
                    }
                }
                if (filtered) item {
                    FilterChip(false, {
                        dateLabel = "Any"
                        vm.update { it.copy(search = "", categoryId = null, city = null, dateFrom = null, dateTo = null) }
                    }, { Text("Clear") }, leadingIcon = { Icon(Icons.Rounded.Close, null, Modifier.size(18.dp)) })
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(s.categories, key = { it.id }) { c ->
                    FilterChip(
                        selected = q.categoryId == c.id,
                        onClick = { vm.update { it.copy(categoryId = if (it.categoryId == c.id) null else c.id) } },
                        label = { Text(c.name) }
                    )
                }
            }
        }
        when {
            s.loading && s.events.isEmpty() -> LoadingView()
            s.error != null && s.events.isEmpty() -> ErrorView(s.error.orEmpty(), vm::retry)
            s.events.isEmpty() -> EmptyView("Nothing found", "No events match your search. Try different words or clear the filters.")
            else -> LazyVerticalGrid(
                GridCells.Adaptive(148.dp), Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val first = s.events.first()
                if (!filtered) item(span = { GridItemSpan(maxLineSpan) }) {
                    FeaturedBanner(first.title, "${Formatters.shortDate(first.eventDate)} · ${first.city}", eventImageUrl(first.imageUrl)) { onEvent(first.id) }
                }
                items(s.events, key = { it.id }) { e ->
                    EventCard(e, names[e.categoryId], e.id in favIds, { onEvent(e.id) }, { favorites.toggle(e.id) })
                }
            }
        }
    }
}

@Composable
private fun FeaturedBanner(title: String, subtitle: String, imageUrl: String?, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(24.dp)).clickable(onClick = onClick)) {
        EventImage(imageUrl, Modifier.fillMaxSize(), null, title)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC070616)))))
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            CosmicTag("Next up")
            Spacer(Modifier.height(6.dp))
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
        }
    }
}

// ===================================================================== EVENT DETAILS

@Composable
fun EventDetailsScreen(eventId: String, favorites: FavoritesViewModel, onBack: () -> Unit, onMap: () -> Unit, onBook: () -> Unit) {
    val vm = appViewModel(key = "ev$eventId") { EventDetailsViewModel(it, eventId) }
    val s by vm.state.collectAsState()
    val favIds by favorites.ids.collectAsState()
    ShowFavouriteMessages(favorites)
    val event = s.event

    GalaxyBackground(GalaxyVariant.DETAILS) {
        when {
            s.loading -> LoadingView()
            event == null -> Column { Spacer(Modifier.height(48.dp)); ErrorView(s.error ?: "Event not found", vm::load, Modifier.fillMaxSize()) }
            else -> {
                val upcoming = Formatters.isUpcoming(event.eventDate)
                val available = s.types.any { it.availableQuantity > 0 }
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Box(Modifier.fillMaxWidth().height(280.dp)) {
                        EventImage(eventImageUrl(event.imageUrl), Modifier.fillMaxSize(), s.categoryIcon, event.title)
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x66070616), Color.Transparent, cosmic.background))))
                        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 40.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            androidx.compose.material3.IconButton(
                                onBack, Modifier.size(48.dp),
                                colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.35f))
                            ) { Icon(androidx.compose.material.icons.Icons.Rounded.ArrowBack, "Back", tint = Color.White) }
                            FavouriteButton(event.id in favIds, event.title, { favorites.toggle(event.id) })
                        }
                    }
                    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        s.categoryName?.let { CosmicTag(it) }
                        Text(event.title, fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp)
                        InfoRow(Icons.Rounded.CalendarMonth, Formatters.date(event.eventDate), "${Formatters.time(event.startTime)} – ${Formatters.time(event.endTime)}")
                        InfoRow(Icons.Rounded.Place, event.venue, "${event.address}, ${event.city}")
                        Text("About this event", style = MaterialTheme.typography.titleLarge)
                        Text(event.description, color = cosmic.textSecondary, fontSize = 15.sp, lineHeight = 22.sp)
                        Text("Tickets", style = MaterialTheme.typography.titleLarge)
                        if (s.types.isEmpty()) Text("Tickets are not on sale yet.", color = cosmic.textSecondary)
                        s.types.forEach { t ->
                            GlassCard(Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(t.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            when {
                                                t.availableQuantity <= 0 -> "Sold out"
                                                t.availableQuantity <= 10 -> "Only ${t.availableQuantity} left"
                                                else -> "${t.availableQuantity} available"
                                            },
                                            color = if (t.availableQuantity <= 10) cosmic.warning else cosmic.textSecondary, fontSize = 12.sp
                                        )
                                    }
                                    Text(if (t.priceCents == 0) "Free" else Formatters.rand(t.priceCents), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = cosmic.violet)
                                }
                            }
                        }
                        OutlineButton("View on map", onMap, icon = Icons.Rounded.Map)
                    }
                }
                Row(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(cosmic.card).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(0.8f)) {
                        Text("From", color = cosmic.textMuted, fontSize = 11.sp)
                        Text(if (event.startingPriceCents == 0) "Free" else Formatters.rand(event.startingPriceCents), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    GradientButton(
                        when { !upcoming -> "Event has passed"; !available -> "Sold out"; else -> "Book now" },
                        onBook, Modifier.weight(1.2f), enabled = upcoming && available
                    )
                }
            }
        }
    }
}

// ===================================================================== EVENT MAP

@SuppressLint("MissingPermission")
private fun lastKnownLocation(context: Context): Location? {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return manager.getProviders(true).mapNotNull { p ->
        try { manager.getLastKnownLocation(p) } catch (e: SecurityException) { null }
    }.maxByOrNull { it.time }
}

@Composable
private fun MapLibreMap(lat: Double, lng: Double, title: String, modifier: Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context)
    }
    DisposableEffect(lifecycle, mapView) {
        var destroyed = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> if (!destroyed) { destroyed = true; mapView.onDestroy() }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            if (!destroyed) { destroyed = true; mapView.onPause(); mapView.onStop(); mapView.onDestroy() }
        }
    }
    AndroidView(
        modifier = modifier,
        factory = {
            mapView.also { mv ->
                mv.getMapAsync { map ->
                    // Free public OpenStreetMap-based tiles (no API key). Swap this URL for another MapLibre style if you prefer.
                    map.setStyle("https://tiles.openfreemap.org/styles/liberty") {
                        val position = LatLng(lat, lng)
                        map.cameraPosition = CameraPosition.Builder().target(position).zoom(13.0).build()
                        map.addMarker(MarkerOptions().position(position).title(title))
                    }
                }
            }
        }
    )
}

@Composable
fun EventMapScreen(eventId: String, onBack: () -> Unit) {
    val vm = appViewModel(key = "map$eventId") { EventDetailsViewModel(it, eventId) }
    val s by vm.state.collectAsState()
    val context = LocalContext.current
    val event = s.event
    var distanceKm by remember { mutableStateOf<Double?>(null) }
    var denied by remember { mutableStateOf(false) }

    fun measure() {
        val here = lastKnownLocation(context)
        val lat = event?.latitude; val lng = event?.longitude
        if (here == null || lat == null || lng == null) {
            context.toast("Could not read your location. Turn on location and try again.")
        } else {
            val out = FloatArray(1)
            Location.distanceBetween(here.latitude, here.longitude, lat, lng, out)
            distanceKm = out[0] / 1000.0
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) { denied = false; measure() } else denied = true
    }

    GalaxyBackground(GalaxyVariant.PLAIN) {
        val lat = event?.latitude; val lng = event?.longitude
        when {
            s.loading -> LoadingView()
            event == null -> ErrorView(s.error ?: "Event not found", vm::load)
            lat == null || lng == null -> Column(Modifier.fillMaxSize()) {
                Spacer(Modifier.height(56.dp))
                EmptyView("No map location", "This event does not have map coordinates yet.", action = { OutlineButton("Back", onBack) })
            }
            else -> {
                MapLibreMap(lat, lng, event.venue, Modifier.fillMaxSize())
                androidx.compose.material3.IconButton(
                    onBack, Modifier.padding(start = 8.dp, top = 40.dp).size(48.dp),
                    colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(containerColor = cosmic.card)
                ) { Icon(androidx.compose.material.icons.Icons.Rounded.ArrowBack, "Back") }
                GlassCard(Modifier.align(Alignment.BottomCenter).padding(16.dp).navigationBarsPadding(), 24.dp) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(event.title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        InfoRow(Icons.Rounded.Place, event.venue, "${event.address}, ${event.city}")
                        InfoRow(Icons.Rounded.Schedule, Formatters.date(event.eventDate), "${Formatters.time(event.startTime)} – ${Formatters.time(event.endTime)}")
                        distanceKm?.let { Text("About ${"%.1f".format(it)} km from you", color = cosmic.violet, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                        if (denied) Text("Location permission was denied. You can still get directions.", color = cosmic.warning, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlineButton("My distance", {
                                permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                            }, Modifier.weight(1f), Icons.Rounded.MyLocation)
                            GradientButton("Directions", {
                                val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
                                try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (e: Exception) { context.toast("No app available to open directions.") }
                            }, Modifier.weight(1f), icon = Icons.Rounded.Directions)
                        }
                    }
                }
            }
        }
    }
}