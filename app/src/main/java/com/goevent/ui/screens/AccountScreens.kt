package com.goevent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Help
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goevent.BuildConfig
import com.goevent.data.ProfileRepository
import com.goevent.data.SupabaseClientManager
import com.goevent.ui.components.GalaxyScreen
import com.goevent.ui.components.GalaxyTextField
import com.goevent.ui.components.GlassCard
import com.goevent.ui.components.GradientButton
import com.goevent.ui.galaxy.GalaxyVariant
import com.goevent.ui.galaxy.GoEventLogo
import com.goevent.ui.theme.cosmic
import com.goevent.ui.viewmodel.SessionViewModel
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    session: SessionViewModel,
    onEdit: () -> Unit,
    onPoints: () -> Unit,
    onInterests: () -> Unit,
    onChangePassword: () -> Unit,
    onSettings: () -> Unit,
    onHelp: () -> Unit,
    onAbout: () -> Unit,
    onSignedOut: () -> Unit
) {
    val ss by session.state.collectAsState()
    val profile = ss.profile

    GalaxyScreen(GalaxyVariant.PROFILE) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Profile", style = MaterialTheme.typography.headlineLarge)
            GlassCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(64.dp).clip(CircleShape).background(cosmic.violet), contentAlignment = Alignment.Center) {
                        Text(profile?.fullName?.take(1)?.uppercase() ?: "U", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(profile?.fullName ?: "User", style = MaterialTheme.typography.titleLarge)
                        Text(profile?.email ?: "", color = cosmic.textSecondary, fontSize = 13.sp)
                    }
                    IconButton(onEdit) { Icon(Icons.Rounded.Edit, "Edit profile") }
                }
            }
            GlassCard(Modifier.fillMaxWidth(), onClick = onPoints) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Star, null, tint = cosmic.warning)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("GoPoints Balance", style = MaterialTheme.typography.titleMedium)
                        Text("${profile?.gopointsBalance ?: 0} points available", color = cosmic.textSecondary, fontSize = 12.sp)
                    }
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = cosmic.textSecondary)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileMenuItem(Icons.Rounded.Favorite, "Interests", onInterests)
                ProfileMenuItem(Icons.Rounded.Lock, "Change Password", onChangePassword)
                ProfileMenuItem(Icons.Rounded.Settings, "Settings", onSettings)
                ProfileMenuItem(Icons.Rounded.Help, "Help & Support", onHelp)
                ProfileMenuItem(Icons.Rounded.Info, "About GoEvent", onAbout)
                ProfileMenuItem(Icons.Rounded.Logout, "Sign Out", { session.signOut(); onSignedOut() }, color = cosmic.error)
            }
        }
    }
}

@Composable
private fun ProfileMenuItem(imageVector: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit, color: androidx.compose.ui.graphics.Color = cosmic.textMain) {
    GlassCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector, null, tint = color)
            Spacer(Modifier.width(16.dp))
            Text(title, color = color, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = cosmic.textSecondary)
        }
    }
}

@Composable
fun EditProfileScreen(session: SessionViewModel, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val ss by session.state.collectAsState()
    val profileRepo = remember { ProfileRepository() }
    var name by remember(ss.profile?.fullName) { mutableStateOf(ss.profile?.fullName.orEmpty()) }
    var saved by remember { mutableStateOf(false) }

    GalaxyScreen(GalaxyVariant.AUTH, "Edit profile", onBack) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            GalaxyTextField(value = name, onValueChange = { name = it }, label = "Full Name", leadingIcon = Icons.Rounded.Person)
            if (saved) Text("Profile updated successfully!", color = cosmic.success)
            GradientButton("Save changes", {
                scope.launch {
                    val uid = SupabaseClientManager.client.auth.currentUserOrNull()?.id
                    if (uid != null) {
                        profileRepo.updateProfile(uid, name)
                        saved = true
                        session.refreshProfile()
                    }
                }
            }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun SettingsScreen(session: SessionViewModel, onBack: () -> Unit, onLanguage: () -> Unit, onChangePassword: () -> Unit) {
    GalaxyScreen(GalaxyVariant.PLAIN, "Settings", onBack) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ProfileMenuItem(Icons.Rounded.Language, "Language", onLanguage)
            ProfileMenuItem(Icons.Rounded.Lock, "Change Password", onChangePassword)
        }
    }
}

@Composable
fun LanguageScreen(session: SessionViewModel, onBack: () -> Unit) {
    GalaxyScreen(GalaxyVariant.PLAIN, "Language", onBack) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("English (Default)", Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
            }
        }
    }
}

private val faqs = listOf(
    "How do I book tickets?" to "Open an event, tap Book now, choose a ticket type and quantity, then follow the checkout steps.",
    "How do GoPoints work?" to "You earn points when you book. 500 points are worth R50 off a booking. Use them at checkout or exchange them for rewards.",
    "How long are my seats held?" to "Seats for a paid booking are held for 15 minutes while you pay. After that they are released for other people.",
    "Where are my tickets?" to "Open the Tickets tab. Tap a ticket to see its QR code and show it at the entrance.",
    "Are payments secure?" to "All transactions are secured and processed through encrypted payment channels.",
    "I forgot my password." to "On the sign-in screen tap Forgot password and we will email you a reset link."
)

@Composable
fun HelpScreen(onBack: () -> Unit) {
    var open by remember { mutableStateOf<Int?>(null) }
    GalaxyScreen(GalaxyVariant.PLAIN, "Help & support", onBack) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Frequently asked questions", style = MaterialTheme.typography.titleLarge)
            faqs.forEachIndexed { i, (q, a) ->
                GlassCard(Modifier.fillMaxWidth(), onClick = { open = if (open == i) null else i }) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(q, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Icon(if (open == i) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
                        }
                        if (open == i) {
                            Spacer(Modifier.height(8.dp))
                            Text(a, color = cosmic.textSecondary, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    GalaxyScreen(GalaxyVariant.AUTH, "About", onBack) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GoEventLogo(Modifier.size(110.dp))
            Text("GoEvent", style = MaterialTheme.typography.headlineLarge)
            Text("Version ${BuildConfig.VERSION_NAME}", color = cosmic.textSecondary)
            Text(
                "GoEvent helps you discover events, book tickets, earn GoPoints and keep your digital tickets in one place.",
                color = cosmic.textSecondary, textAlign = TextAlign.Center
            )
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Built with", style = MaterialTheme.typography.titleMedium)
                    Text("Kotlin and Jetpack Compose · Firebase Authentication · Supabase (database and storage) · MapLibre · ZXing · Coil", color = cosmic.textSecondary, fontSize = 13.sp)
                    Text("Secure payment processing and encrypted digital tickets.", color = cosmic.textMuted, fontSize = 12.sp)
                }
            }
        }
    }
}
