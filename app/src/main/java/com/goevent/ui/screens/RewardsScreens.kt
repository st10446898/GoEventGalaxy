package com.goevent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.GridItemSpan
import com.goevent.core.model.Achievement
import com.goevent.core.model.NotificationItem
import com.goevent.core.model.Reward
import com.goevent.core.util.Formatters
import com.goevent.ui.components.CosmicTag
import com.goevent.ui.components.EmptyView
import com.goevent.ui.components.ErrorText
import com.goevent.ui.components.ErrorView
import com.goevent.ui.components.EventCard
import com.goevent.ui.components.EventImage
import com.goevent.ui.components.GalaxyScreen
import com.goevent.ui.components.GlassCard
import com.goevent.ui.components.GradientButton
import com.goevent.ui.components.LoadingView
import com.goevent.ui.components.OutlineButton
import com.goevent.ui.components.SectionHeader
import com.goevent.ui.components.categoryIcon
import com.goevent.ui.components.rewardImageUrl
import com.goevent.ui.components.toast
import com.goevent.ui.galaxy.GalaxyVariant
import com.goevent.ui.theme.Galaxy
import com.goevent.ui.theme.cosmic
import com.goevent.ui.viewmodel.FavoritesViewModel
import com.goevent.ui.viewmodel.NotificationsViewModel
import com.goevent.ui.viewmodel.PointsViewModel
import com.goevent.ui.viewmodel.SavedViewModel
import com.goevent.ui.viewmodel.SessionViewModel
import com.goevent.ui.viewmodel.appViewModel

// ===================================================================== SAVED

@Composable
fun SavedScreen(favorites: FavoritesViewModel, onEvent: (String) -> Unit, onExplore: () -> Unit) {
    val vm = appViewModel { SavedViewModel(it) }
    val s by vm.state.collectAsState()
    val ids by favorites.ids.collectAsState()
    val ctx = LocalContext.current
    val msg by favorites.message.collectAsState()
    LaunchedEffect(msg) { msg?.let { ctx.toast(it); favorites.clearMessage() } }
    LaunchedEffect(ids) { if (ids.isNotEmpty()) vm.load(ids) }
    val shown = s.events.filter { it.id in ids }

    GalaxyScreen(GalaxyVariant.SAVED) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(8.dp))
            Text("Saved", style = MaterialTheme.typography.headlineLarge)
            Text("${ids.size} saved event${if (ids.size == 1) "" else "s"}", color = cosmic.textSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
        }
        when {
            ids.isEmpty() -> EmptyView("Nothing saved yet", "Tap the heart on any event to keep it here.", action = { GradientButton("Discover events", onExplore) })
            s.loading && shown.isEmpty() -> LoadingView()
            s.error != null && shown.isEmpty() -> ErrorView(s.error.orEmpty(), { vm.load(ids) })
            else -> LazyVerticalGrid(
                GridCells.Adaptive(148.dp), contentPadding = PaddingValues(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                gridItems(shown, key = { it.id }) { e ->
                    EventCard(e, null, true, { onEvent(e.id) }, { favorites.toggle(e.id) })
                }
            }
        }
    }
}

// ===================================================================== GOPOINTS

@Composable
fun GoPointsScreen(session: SessionViewModel, onBack: () -> Unit, onHistory: () -> Unit, onRewards: () -> Unit, onAchievements: () -> Unit) {
    val vm = appViewModel(key = "points") { PointsViewModel(it) }
    val s by vm.state.collectAsState()
    val ss by session.state.collectAsState()
    LaunchedEffect(ss.profile?.id) { vm.load(); session.refreshProfile() }
    val balance = ss.profile?.gopointsBalance ?: 0
    val unlocked = s.userAchievements.count { it.isUnlocked }

    GalaxyScreen(GalaxyVariant.POINTS, "GoPoints", onBack) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Galaxy.galaxy).padding(24.dp)) {
                Column {
                    Text("Your balance", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                    Text("$balance", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                    Text("Worth ${Formatters.rand(balance * 10)} off your bookings", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
                }
            }
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("How it works", style = MaterialTheme.typography.titleMedium)
                    Text("• Earn points when you book events.\n• 500 points = R50 off a booking.\n• Unlock achievements for bonus points.", color = cosmic.textSecondary, fontSize = 14.sp, lineHeight = 21.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlineButton("Rewards", onRewards, Modifier.weight(1f), Icons.Rounded.CardGiftcard)
                OutlineButton("History", onHistory, Modifier.weight(1f), Icons.Rounded.History)
            }
            OutlineButton(if (s.achievements.isEmpty()) "Achievements" else "Achievements ($unlocked/${s.achievements.size})", onAchievements, Modifier.fillMaxWidth(), Icons.Rounded.EmojiEvents)
            SectionHeader("Recent activity")
            when {
                s.loading && s.history.isEmpty() -> Text("Loading…", color = cosmic.textSecondary)
                s.error != null && s.history.isEmpty() -> ErrorText(s.error.orEmpty())
                s.history.isEmpty() -> Text("No points activity yet. Book your first event to start earning.", color = cosmic.textSecondary)
                else -> s.history.take(5).forEach { h -> HistoryRow(h.reason, h.points, h.transactionType, h.createdAt) }
            }
        }
    }
}

@Composable
private fun HistoryRow(reason: String, points: Int, type: String, createdAt: String?) {
    val earned = points >= 0 && !type.equals("REDEEMED", true)
    GlassCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (earned) Icons.Rounded.AddCircle else Icons.Rounded.RemoveCircle, null, tint = if (earned) cosmic.success else cosmic.pink, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(reason, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(Formatters.relativeTime(createdAt), color = cosmic.textMuted, fontSize = 12.sp)
            }
            Text(if (points > 0) "+$points" else "$points", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (earned) cosmic.success else cosmic.pink)
        }
    }
}

@Composable
fun GoPointsHistoryScreen(session: SessionViewModel, onBack: () -> Unit) {
    val vm = appViewModel(key = "pointsHistory") { PointsViewModel(it) }
    val s by vm.state.collectAsState()
    val ss by session.state.collectAsState()
    LaunchedEffect(ss.profile?.id) { vm.load() }
    GalaxyScreen(GalaxyVariant.POINTS, "Points history", onBack) {
        when {
            s.loading && s.history.isEmpty() -> LoadingView()
            s.error != null && s.history.isEmpty() -> ErrorView(s.error.orEmpty(), { vm.load() })
            s.history.isEmpty() -> EmptyView("No activity yet", "Your earned and spent points will be listed here.")
            else -> LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(s.history, key = { it.id }) { h -> HistoryRow(h.reason, h.points, h.transactionType, h.createdAt) }
            }
        }
    }
}

// ===================================================================== REWARDS

@Composable
fun RewardsScreen(session: SessionViewModel, onBack: () -> Unit) {
    val vm = appViewModel(key = "rewards") { PointsViewModel(it) }
    val s by vm.state.collectAsState()
    val ss by session.state.collectAsState()
    var confirm by remember { mutableStateOf<Reward?>(null) }
    LaunchedEffect(ss.profile?.id) { vm.load() }
    val balance = ss.profile?.gopointsBalance ?: 0

    GalaxyScreen(GalaxyVariant.POINTS, "Rewards", onBack) {
        Row(Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Star, null, tint = cosmic.warning)
            Spacer(Modifier.width(8.dp))
            Text("$balance points available", fontWeight = FontWeight.SemiBold)
        }
        s.redeemError?.let { Box(Modifier.padding(horizontal = 20.dp)) { ErrorText(it) } }
        when {
            s.loading && s.rewards.isEmpty() -> LoadingView()
            s.error != null && s.rewards.isEmpty() -> ErrorView(s.error.orEmpty(), { vm.load() })
            s.rewards.isEmpty() && s.redemptions.isEmpty() -> EmptyView("No rewards yet", "New rewards will appear here soon.")
            else -> LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items(s.rewards, key = { it.id }) { r ->
                    val affordable = balance >= r.pointsCost
                    GlassCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            EventImage(rewardImageUrl(r.imageUrl), Modifier.size(72.dp).clip(RoundedCornerShape(16.dp)), "CardGiftcard", r.title)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(r.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(r.description, color = cosmic.textSecondary, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(4.dp))
                                Text("${r.pointsCost} points", color = cosmic.violet, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(Modifier.width(8.dp))
                            GradientButton(if (affordable) "Redeem" else "Need ${r.pointsCost - balance}", { confirm = r }, Modifier.width(104.dp).height(44.dp), enabled = affordable && !s.redeeming)
                        }
                    }
                }
                if (s.redemptions.isNotEmpty()) {
                    item { SectionHeader("My redemptions") }
                    items(s.redemptions, key = { "red-" + it.id }) { red ->
                        val title = s.rewards.firstOrNull { it.id == red.rewardId }?.title ?: "Reward"
                        GlassCard(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(title, style = MaterialTheme.typography.titleMedium)
                                    Text("Code: ${red.redemptionCode}", color = cosmic.violet, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(Formatters.relativeTime(red.redeemedAt), color = cosmic.textMuted, fontSize = 12.sp)
                                }
                                CosmicTag(red.status, color = if (red.status == "Active") cosmic.success else cosmic.textMuted)
                            }
                        }
                    }
                }
            }
        }
    }

    confirm?.let { r ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Redeem ${r.title}?") },
            text = { Text("This will spend ${r.pointsCost} GoPoints. Your balance is checked securely by our server.") },
            confirmButton = { TextButton({ confirm = null; vm.redeem(r) { session.refreshProfile() } }) { Text("Redeem") } },
            dismissButton = { TextButton({ confirm = null }) { Text("Cancel") } },
            containerColor = cosmic.card
        )
    }
    s.lastRedemption?.let { done ->
        AlertDialog(
            onDismissRequest = vm::dismissRedemption,
            title = { Text("Reward redeemed") },
            text = { Text("${done.title}\nYour code: ${done.redemptionCode}\n\nShow this code when you use your reward. It is also saved under My redemptions.") },
            confirmButton = { TextButton(vm::dismissRedemption) { Text("Done") } },
            containerColor = cosmic.card
        )
    }
}

// ===================================================================== ACHIEVEMENTS

private fun achievementIcon(name: String): ImageVector = when (name) {
    "Favorite" -> Icons.Rounded.Favorite
    "EmojiEvents" -> Icons.Rounded.EmojiEvents
    "CardGiftcard" -> Icons.Rounded.CardGiftcard
    else -> categoryIcon(name)
}

@Composable
fun AchievementsScreen(session: SessionViewModel, onBack: () -> Unit) {
    val vm = appViewModel(key = "achievements") { PointsViewModel(it) }
    val s by vm.state.collectAsState()
    val ss by session.state.collectAsState()
    LaunchedEffect(ss.profile?.id) { vm.load(refreshAchievements = true); session.refreshProfile() }
    val progress = remember(s.userAchievements) { s.userAchievements.associateBy { it.achievementId } }

    GalaxyScreen(GalaxyVariant.POINTS, "Achievements", onBack) {
        when {
            s.loading && s.achievements.isEmpty() -> LoadingView()
            s.error != null && s.achievements.isEmpty() -> ErrorView(s.error.orEmpty(), { vm.load(true) })
            s.achievements.isEmpty() -> EmptyView("No achievements yet", "Achievements will appear here soon.")
            else -> LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(s.achievements, key = { it.id }) { a -> AchievementCard(a, progress[a.id]?.currentProgress ?: 0, progress[a.id]?.isUnlocked == true) }
            }
        }
    }
}

@Composable
private fun AchievementCard(a: Achievement, current: Int, unlocked: Boolean) {
    GlassCard(Modifier.fillMaxWidth().alpha(if (unlocked) 1f else 0.85f)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(if (unlocked) Galaxy.galaxy else Galaxy.nebula), contentAlignment = Alignment.Center) {
                Icon(if (unlocked) achievementIcon(a.iconName) else Icons.Rounded.Lock, null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(a.title, style = MaterialTheme.typography.titleMedium)
                Text(a.description, color = cosmic.textSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                val shown = if (unlocked) a.requirementThreshold else current.coerceAtMost(a.requirementThreshold)
                LinearProgressIndicator(
                    progress = { if (a.requirementThreshold <= 0) 1f else shown.toFloat() / a.requirementThreshold },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = cosmic.violet, trackColor = cosmic.border
                )
                Text(if (unlocked) "Unlocked · +${a.rewardPoints} points" else "$shown / ${a.requirementThreshold} · +${a.rewardPoints} points", color = if (unlocked) cosmic.success else cosmic.textMuted, fontSize = 11.sp)
            }
        }
    }
}

// ===================================================================== NOTIFICATIONS

@Composable
fun NotificationsScreen(session: SessionViewModel, onBack: () -> Unit) {
    val vm = appViewModel(key = "notifications") { NotificationsViewModel(it) }
    val s by vm.state.collectAsState()
    val ss by session.state.collectAsState()
    LaunchedEffect(ss.profile?.id) { vm.load() }
    val hasUnread = s.items.any { !it.isRead }

    GalaxyScreen(GalaxyVariant.PLAIN, "Notifications", onBack, actions = {
        if (hasUnread) TextButton(vm::markAllRead) { Text("Mark all read", color = cosmic.violet) }
    }) {
        when {
            s.loading && s.items.isEmpty() -> LoadingView()
            s.error != null && s.items.isEmpty() -> ErrorView(s.error.orEmpty(), vm::load)
            s.items.isEmpty() -> EmptyView("All caught up", "Booking, points and event updates will appear here.")
            else -> LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(s.items, key = { it.id }) { n -> NotificationCard(n) { vm.markRead(n.id) } }
            }
        }
    }
}

@Composable
private fun NotificationCard(n: NotificationItem, onClick: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth(), onClick = if (n.isRead) null else onClick) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.padding(top = 4.dp).size(10.dp).clip(CircleShape).background(if (n.isRead) Color.Transparent else cosmic.pink))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(n.title, style = MaterialTheme.typography.titleMedium, fontWeight = if (n.isRead) FontWeight.Normal else FontWeight.Bold)
                Text(n.message, color = cosmic.textSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Text(Formatters.relativeTime(n.createdAt), color = cosmic.textMuted, fontSize = 11.sp)
            }
        }
    }
}