package com.goevent.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import com.goevent.core.util.Validators
import com.goevent.ui.components.CategoryBubble
import com.goevent.ui.components.ErrorText
import com.goevent.ui.components.ErrorView
import com.goevent.ui.components.GalaxyScreen
import com.goevent.ui.components.GalaxyTextField
import com.goevent.ui.components.GradientButton
import com.goevent.ui.components.LoadingView
import com.goevent.ui.components.OutlineButton
import com.goevent.ui.galaxy.GalaxyBackground
import com.goevent.ui.galaxy.GalaxyVariant
import com.goevent.ui.galaxy.GoEventLogo
import com.goevent.ui.galaxy.MoonIllustration
import com.goevent.ui.galaxy.PlanetIllustration
import com.goevent.ui.galaxy.SpiralGalaxy
import com.goevent.ui.navigation.Routes
import com.goevent.ui.theme.cosmic
import com.goevent.ui.viewmodel.InterestsViewModel
import com.goevent.ui.viewmodel.SessionViewModel
import com.goevent.ui.viewmodel.appViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ------------------------------------------------------------------ splash

@Composable
fun SplashScreen(session: SessionViewModel, onNavigate: (String) -> Unit) {
    val s by session.state.collectAsState()
    var minimumShown by remember { mutableStateOf(false) }
    val reveal = remember { Animatable(0f) }

    LaunchedEffect(Unit) { reveal.animateTo(1f, tween(900)) }
    LaunchedEffect(Unit) { delay(2000); minimumShown = true }
    // Go on as soon as BOTH the 2 s animation and the login restore have finished.
    LaunchedEffect(minimumShown, s.restoring, s.prefsLoaded, s.restoreFailed, s.profile) {
        if (minimumShown && !s.restoring && s.prefsLoaded && !s.restoreFailed) {
            onNavigate(
                when {
                    s.profile != null -> Routes.HOME
                    !s.onboardingDone -> Routes.ONBOARDING
                    else -> Routes.SIGN_IN
                }
            )
        }
    }

    GalaxyBackground(GalaxyVariant.AUTH) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
        ) {
            Box(Modifier.alpha(reveal.value).scale(0.8f + 0.2f * reveal.value)) { GoEventLogo(Modifier.size(110.dp)) }
            Spacer(Modifier.height(16.dp))
            Text("GoEvent", style = MaterialTheme.typography.displayLarge, modifier = Modifier.alpha(reveal.value))
            Text("Explore beyond the ordinary", color = cosmic.textSecondary, fontSize = 14.sp, modifier = Modifier.alpha(reveal.value))
            Spacer(Modifier.height(40.dp))
            if (s.restoreFailed) {
                Text(s.restoreError ?: "We could not load your account.", color = cosmic.error, textAlign = TextAlign.Center, fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                GradientButton("Try again", { session.restore() }, Modifier.widthIn(max = 320.dp))
                Spacer(Modifier.height(8.dp))
                OutlineButton("Sign out", { session.signOut(); session.restore() }, Modifier.widthIn(max = 320.dp))
            } else {
                CircularProgressIndicator(color = cosmic.violet, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
            }
        }
    }
}

// ------------------------------------------------------------------ onboarding

private data class OnboardingPage(val title: String, val body: String)

@Composable
fun OnboardingScreen(session: SessionViewModel, onFinish: () -> Unit) {
    val pages = listOf(
        OnboardingPage("Discover the Universe of Events", "Concerts, tech summits, sport, art and food, all across South Africa in one galaxy."),
        OnboardingPage("Find Your Next Experience", "Search, filter by city and date, and save the events you love."),
        OnboardingPage("Book Your Place in the Universe", "Secure tickets in seconds, earn GoPoints and show your QR code at the gate.")
    )
    val pager = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pages.lastIndex
    fun finish() { session.completeOnboarding(); onFinish() }

    GalaxyBackground(GalaxyVariant.PLAIN) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(56.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (!last) TextButton({ finish() }) { Text("Skip", color = cosmic.textSecondary) }
            }
            HorizontalPager(pager, Modifier.weight(1f)) { page ->
                Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().height(320.dp), contentAlignment = Alignment.Center) {
                        when (page) {
                            0 -> SpiralGalaxy(Modifier.size(300.dp), seed = 21)
                            1 -> {
                                Canvas(Modifier.size(300.dp)) {
                                    val c = center
                                    listOf(0.46f, 0.34f).forEach { f ->
                                        drawOval(Color(0x558B5CF6), Offset(c.x - size.width * f, c.y - size.height * f * 0.35f), Size(size.width * f * 2, size.height * f * 0.7f), style = Stroke(density * 1.5f))
                                    }
                                }
                                PlanetIllustration(Modifier.size(260.dp))
                            }
                            else -> {
                                PlanetIllustration(
                                    Modifier.size(220.dp).align(Alignment.BottomStart),
                                    colors = listOf(Color(0xFFF9A8D4), Color(0xFFDB2777), Color(0xFF500724)), hasRing = false
                                )
                                MoonIllustration(Modifier.size(200.dp).align(Alignment.TopEnd), crescent = false)
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(pages[page].title, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(pages[page].body, color = cosmic.textSecondary, fontSize = 15.sp, textAlign = TextAlign.Center)
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center) {
                repeat(pages.size) { i ->
                    Box(
                        Modifier.padding(4.dp).size(width = if (i == pager.currentPage) 22.dp else 8.dp, height = 8.dp)
                            .clip(CircleShape).background(if (i == pager.currentPage) cosmic.violet else cosmic.border)
                    )
                }
            }
            GradientButton(
                if (last) "Get started" else "Continue",
                { if (last) finish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }
    }
}

// ------------------------------------------------------------------ auth panel

@Composable
private fun AuthPanel(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    GalaxyBackground(GalaxyVariant.AUTH) {
        BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Column(
                Modifier.fillMaxWidth().heightIn(min = maxHeight).verticalScroll(rememberScrollState()).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
            ) {
                GoEventLogo(Modifier.size(84.dp))
                Spacer(Modifier.height(12.dp))
                val shape = RoundedCornerShape(24.dp)
                Column(
                    Modifier.widthIn(max = 440.dp).fillMaxWidth().clip(shape)
                        .background(cosmic.card.copy(alpha = 0.93f), shape).border(1.dp, cosmic.border, shape).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(title, style = MaterialTheme.typography.headlineLarge)
                    Text(subtitle, color = cosmic.textSecondary, fontSize = 14.sp)
                    content()
                }
            }
        }
    }
}

@Composable
fun SignInScreen(session: SessionViewModel, onSuccess: () -> Unit, onRegister: () -> Unit, onForgot: () -> Unit) {
    val s by session.state.collectAsState()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var attempted by remember { mutableStateOf(false) }
    val emailError = Validators.emailError(email)
    val passError = if (password.isEmpty()) "Password is required" else null
    LaunchedEffect(Unit) { session.clearMessages() }
    fun submit() {
        attempted = true
        if (emailError == null && passError == null) session.signIn(email, password, onSuccess)
    }
    AuthPanel("Welcome back", "Sign in to explore events across the universe.") {
        GalaxyTextField(email, { email = it }, "Email", leadingIcon = Icons.Rounded.Email, error = if (attempted) emailError else null, keyboardType = KeyboardType.Email)
        GalaxyTextField(password, { password = it }, "Password", leadingIcon = Icons.Rounded.Lock, error = if (attempted) passError else null, isPassword = true, imeAction = ImeAction.Done, onImeAction = { submit() })
        TextButton(onForgot, Modifier.align(Alignment.End)) { Text("Forgot password?", color = cosmic.violet) }
        s.error?.let { ErrorText(it) }
        GradientButton("Sign in", { submit() }, loading = s.busy)
        OutlineButton("Continue with Google", { session.signInWithGoogle(onSuccess) }, Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("New to GoEvent?", color = cosmic.textSecondary, fontSize = 14.sp)
            TextButton(onRegister) { Text("Create an account", color = cosmic.violet) }
        }
    }
}

@Composable
fun RegisterScreen(session: SessionViewModel, onSuccess: () -> Unit, onSignIn: () -> Unit) {
    val s by session.state.collectAsState()
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var attempted by remember { mutableStateOf(false) }
    val nameError = Validators.nameError(name)
    val emailError = Validators.emailError(email)
    val passError = Validators.passwordError(password)
    val confirmError = Validators.confirmPasswordError(password, confirm)
    LaunchedEffect(Unit) { session.clearMessages() }
    fun submit() {
        attempted = true
        if (listOf(nameError, emailError, passError, confirmError).all { it == null }) session.register(name.trim(), email, password, onSuccess)
    }
    AuthPanel("Join GoEvent", "Create your account and get 100 welcome GoPoints.") {
        GalaxyTextField(name, { name = it }, "Full name", leadingIcon = Icons.Rounded.Person, error = if (attempted) nameError else null)
        GalaxyTextField(email, { email = it }, "Email", leadingIcon = Icons.Rounded.Email, error = if (attempted) emailError else null, keyboardType = KeyboardType.Email)
        GalaxyTextField(password, { password = it }, "Password", leadingIcon = Icons.Rounded.Lock, error = if (attempted) passError else null, isPassword = true)
        GalaxyTextField(confirm, { confirm = it }, "Confirm password", leadingIcon = Icons.Rounded.Lock, error = if (attempted) confirmError else null, isPassword = true, imeAction = ImeAction.Done, onImeAction = { submit() })
        s.error?.let { ErrorText(it) }
        GradientButton("Create account", { submit() }, loading = s.busy)
        OutlineButton("Continue with Google", { session.signInWithGoogle(onSuccess) }, Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("Already have an account?", color = cosmic.textSecondary, fontSize = 14.sp)
            TextButton(onSignIn) { Text("Sign in", color = cosmic.violet) }
        }
    }
}

@Composable
fun ForgotPasswordScreen(session: SessionViewModel, onBack: () -> Unit) {
    val s by session.state.collectAsState()
    var email by rememberSaveable { mutableStateOf("") }
    var attempted by remember { mutableStateOf(false) }
    val emailError = Validators.emailError(email)
    LaunchedEffect(Unit) { session.clearMessages() }
    fun submit() { attempted = true; if (emailError == null) session.sendPasswordReset(email) }
    AuthPanel("Reset your password", "Enter your email and we will send you a secure reset link.") {
        GalaxyTextField(email, { email = it }, "Email", leadingIcon = Icons.Rounded.Email, error = if (attempted) emailError else null, keyboardType = KeyboardType.Email, imeAction = ImeAction.Done, onImeAction = { submit() })
        s.error?.let { ErrorText(it) }
        s.info?.let { Text(it, color = cosmic.success, fontSize = 13.sp) }
        GradientButton("Send reset link", { submit() }, loading = s.busy)
        TextButton(onBack, Modifier.align(Alignment.CenterHorizontally)) { Text("Back to sign in", color = cosmic.violet) }
    }
}

/** In-app password change (Firebase sends reset links by email; this is the "Reset password" screen for signed-in users). */
@Composable
fun ChangePasswordScreen(session: SessionViewModel, onBack: () -> Unit) {
    val s by session.state.collectAsState()
    var current by rememberSaveable { mutableStateOf("") }
    var new by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var attempted by remember { mutableStateOf(false) }
    val currentError = if (current.isEmpty()) "Enter your current password" else null
    val newError = Validators.passwordError(new)
    val confirmError = Validators.confirmPasswordError(new, confirm)
    LaunchedEffect(Unit) { session.clearMessages() }
    fun submit() {
        attempted = true
        if (currentError == null && newError == null && confirmError == null) session.changePassword(current, new)
    }
    GalaxyScreen(GalaxyVariant.AUTH, "Change password", onBack) {
        Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Choose a strong new password with at least 8 characters, upper and lower case letters and a number.", color = cosmic.textSecondary, fontSize = 14.sp)
            GalaxyTextField(current, { current = it }, "Current password", leadingIcon = Icons.Rounded.Lock, error = if (attempted) currentError else null, isPassword = true)
            GalaxyTextField(new, { new = it }, "New password", leadingIcon = Icons.Rounded.Lock, error = if (attempted) newError else null, isPassword = true)
            GalaxyTextField(confirm, { confirm = it }, "Confirm new password", leadingIcon = Icons.Rounded.Lock, error = if (attempted) confirmError else null, isPassword = true, imeAction = ImeAction.Done, onImeAction = { submit() })
            s.error?.let { ErrorText(it) }
            s.info?.let { Text(it, color = cosmic.success, fontSize = 13.sp) }
            GradientButton("Update password", { submit() }, loading = s.busy)
        }
    }
}

// ------------------------------------------------------------------ interests

@Composable
fun InterestSelectionScreen(onDone: () -> Unit) {
    val vm = appViewModel { InterestsViewModel(it) }
    val s by vm.state.collectAsState()
    GalaxyScreen(GalaxyVariant.EXPLORE) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(16.dp))
            Text("What are you into?", style = MaterialTheme.typography.headlineLarge)
            Text("Pick the categories you love. You can change this any time in your profile.", color = cosmic.textSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))
            when {
                s.loading -> LoadingView(Modifier.weight(1f).fillMaxWidth())
                s.categories.isEmpty() -> ErrorView(s.error ?: "No categories found.", vm::load, Modifier.weight(1f).fillMaxWidth())
                else -> LazyVerticalGrid(
                    GridCells.Fixed(3), Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(s.categories, key = { it.id }) { cat ->
                        CategoryBubble(cat.name, cat.iconName, cat.id in s.selected, { vm.toggle(cat.id) }, Modifier.fillMaxWidth())
                    }
                }
            }
            s.error?.takeIf { s.categories.isNotEmpty() }?.let { ErrorText(it) }
            GradientButton(
                if (s.selected.isEmpty()) "Continue" else "Save ${s.selected.size} and continue",
                { if (s.selected.isEmpty()) onDone() else vm.save(onDone) },
                Modifier.padding(vertical = 8.dp), loading = s.saving
            )
            TextButton(onDone, Modifier.align(Alignment.CenterHorizontally).navigationBarsPadding()) { Text("Skip for now", color = cosmic.textSecondary) }
        }
    }
}