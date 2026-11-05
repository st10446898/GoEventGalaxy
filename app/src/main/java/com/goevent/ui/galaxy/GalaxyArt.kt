package com.goevent.ui.galaxy

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.goevent.ui.theme.cosmic
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

enum class GalaxyVariant { HOME, AUTH, EXPLORE, TICKETS, SAVED, POINTS, PROFILE, DETAILS, PLAIN }

/** True when the user switched system animations off (accessibility / battery saver). */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        try {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        } catch (e: Exception) {
            false
        }
    }
}

// ---------------------------------------------------------------- stars

private class Star(val x: Float, val y: Float, val r: Float, val alpha: Float, val phase: Float, val sparkle: Boolean)

@Composable
fun StarField(modifier: Modifier = Modifier, count: Int = 240, seed: Int = 7, animate: Boolean = true) {
    val dark = cosmic.isDark
    val reduced = rememberReducedMotion()
    val stars = remember(count, seed) {
        val rnd = Random(seed)
        List(count) {
            val roll = rnd.nextFloat()
            val radius = when {
                roll > 0.97f -> 2.6f + rnd.nextFloat() * 1.8f   // large glowing
                roll > 0.80f -> 1.4f + rnd.nextFloat()          // medium
                else -> 0.5f + rnd.nextFloat() * 0.8f           // small / dust
            }
            Star(rnd.nextFloat(), rnd.nextFloat(), radius, 0.15f + rnd.nextFloat() * 0.85f, rnd.nextFloat() * 6.28f, roll > 0.985f)
        }
    }
    val transition = rememberInfiniteTransition(label = "stars")
    val t by transition.animateFloat(
        initialValue = 0f, targetValue = 6.2832f,
        animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing)), label = "twinkle"
    )
    val color = if (dark) Color.White else Color(0xFF6D28D9)
    val strength = if (dark) 1f else 0.45f
    Canvas(modifier.fillMaxSize()) {
        val time = if (animate && !reduced) t else 0f
        stars.forEach { s ->
            val a = (s.alpha * (0.65f + 0.35f * sin(time + s.phase))).coerceIn(0.05f, 1f) * strength
            val c = Offset(s.x * size.width, s.y * size.height)
            val rad = s.r * density
            if (s.r > 2f) drawCircle(color.copy(alpha = a * 0.25f), rad * 3.2f, c)
            drawCircle(color.copy(alpha = a), rad, c)
            if (s.sparkle) {
                val len = rad * 4.5f
                drawLine(color.copy(alpha = a), Offset(c.x - len, c.y), Offset(c.x + len, c.y), strokeWidth = density)
                drawLine(color.copy(alpha = a), Offset(c.x, c.y - len), Offset(c.x, c.y + len), strokeWidth = density)
            }
        }
    }
}

@Composable
fun NebulaGlow(
    modifier: Modifier = Modifier,
    colors: List<Color>,
    center: Offset = Offset(0.5f, 0.5f),
    radiusFraction: Float = 0.6f,
    alpha: Float = 0.35f
) {
    val strength = if (cosmic.isDark) 1f else 0.5f
    Canvas(modifier) {
        val base = Offset(size.width * center.x, size.height * center.y)
        val r = maxOf(size.width, size.height) * radiusFraction
        colors.forEachIndexed { i, col ->
            val c = Offset(base.x + (i - 1) * r * 0.25f, base.y + (if (i % 2 == 0) -1 else 1) * r * 0.18f)
            val rad = r * (1f - i * 0.15f)
            drawCircle(Brush.radialGradient(listOf(col.copy(alpha = alpha * strength), Color.Transparent), c, rad), rad, c)
        }
    }
}

// ---------------------------------------------------------------- bodies

@Composable
fun PlanetIllustration(
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(Color(0xFFC4B5FD), Color(0xFF8B5CF6), Color(0xFF4C1D95)),
    hasRing: Boolean = true
) {
    Canvas(modifier) {
        val c = center
        val pr = min(size.width, size.height) * 0.26f
        drawCircle(Brush.radialGradient(listOf(colors[1].copy(alpha = 0.35f), Color.Transparent), c, pr * 2f), pr * 2f, c)

        fun androidx.compose.ui.graphics.drawscope.DrawScope.ring() {
            rotate(-18f, c) {
                drawOval(Color(0xCCE9D5FF), Offset(c.x - pr * 1.9f, c.y - pr * 0.42f), Size(pr * 3.8f, pr * 0.84f), style = Stroke(pr * 0.14f))
                drawOval(Color(0x66F0ABFC), Offset(c.x - pr * 1.6f, c.y - pr * 0.34f), Size(pr * 3.2f, pr * 0.68f), style = Stroke(pr * 0.05f))
            }
        }
        if (hasRing) ring()

        drawCircle(
            Brush.radialGradient(colors, Offset(c.x - pr * 0.35f, c.y - pr * 0.4f), pr * 1.6f),
            pr, c
        )
        val disc = Path().apply { addOval(Rect(c, pr)) }
        clipPath(disc) {
            for (i in 0..5) {
                drawRect(
                    Color.Black.copy(alpha = 0.06f + (i % 2) * 0.05f),
                    Offset(c.x - pr, c.y - pr + pr * 0.36f * i + pr * 0.1f),
                    Size(pr * 2f, pr * 0.14f)
                )
            }
            drawCircle(
                Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)), Offset(c.x - pr * 0.5f, c.y - pr * 0.5f), pr * 2f),
                pr, c
            )
        }
        if (hasRing) clipRect(top = c.y) { ring() }
    }
}

@Composable
fun MoonIllustration(modifier: Modifier = Modifier, crescent: Boolean = false) {
    Canvas(modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        val c = center
        val r = min(size.width, size.height) * 0.34f
        drawCircle(
            Brush.radialGradient(
                listOf(Color.White, Color(0xFFD8D2F0), Color(0xFFA9A2C9)),
                Offset(c.x - r * 0.3f, c.y - r * 0.3f), r * 1.5f
            ),
            r, c
        )
        listOf(
            Triple(-0.35f, -0.2f, 0.18f), Triple(0.25f, 0.3f, 0.22f),
            Triple(0.35f, -0.35f, 0.12f), Triple(-0.1f, 0.45f, 0.1f)
        ).forEach { (dx, dy, rr) ->
            val p = Offset(c.x + r * dx, c.y + r * dy)
            drawCircle(Color(0x33504A78), r * rr, p)
            drawCircle(Color(0x44FFFFFF), r * rr, p, style = Stroke(density))
        }
        if (crescent) {
            drawCircle(Color.Black, r * 0.92f, Offset(c.x + r * 0.55f, c.y - r * 0.1f), blendMode = BlendMode.Clear)
        }
        drawCircle(
            Brush.radialGradient(listOf(Color(0x66E9E4FF), Color.Transparent), c, r * 2f),
            r * 2f, c, blendMode = BlendMode.DstOver
        )
    }
}

@Composable
fun SunIllustration(modifier: Modifier = Modifier) {
    val reduced = rememberReducedMotion()
    val transition = rememberInfiniteTransition(label = "sun")
    val spin by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(40_000, easing = LinearEasing)), label = "spin"
    )
    Canvas(modifier) {
        val c = center
        val r = min(size.width, size.height) * 0.2f
        drawCircle(Brush.radialGradient(listOf(Color(0x88FFF1A8), Color(0x33F59E0B), Color.Transparent), c, r * 3.2f), r * 3.2f, c)
        val rot = if (reduced) 0f else spin
        for (i in 0 until 16) {
            val ang = Math.toRadians((i * 22.5f + rot).toDouble())
            val inner = r * 1.25f
            val outer = r * (1.65f + (i % 2) * 0.45f)
            drawLine(
                Color(0x99FDE68A),
                Offset(c.x + cos(ang).toFloat() * inner, c.y + sin(ang).toFloat() * inner),
                Offset(c.x + cos(ang).toFloat() * outer, c.y + sin(ang).toFloat() * outer),
                strokeWidth = density * 2.5f, cap = StrokeCap.Round
            )
        }
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF7AE), Color(0xFFFBBF24), Color(0xFFF97316)), c, r), r, c)
    }
}

private class Dot(val x: Float, val y: Float, val r: Float, val color: Color)

@Composable
fun SpiralGalaxy(modifier: Modifier = Modifier, seed: Int = 3) {
    val dots = remember(seed) {
        val rnd = Random(seed)
        val list = ArrayList<Dot>()
        for (arm in 0 until 3) for (i in 0 until 260) {
            val t = i / 260f
            val ang = t * 5.4f + arm * (2f * PI.toFloat() / 3f)
            val rad = 0.08f + t * 0.92f
            val spread = (rnd.nextFloat() - 0.5f) * 0.16f * (0.4f + t)
            val col = when {
                t < 0.25f -> Color(0xFFFBCFE8)
                t < 0.6f -> Color(0xFFC084FC)
                else -> Color(0xFF60A5FA)
            }
            list.add(Dot(cos(ang) * rad + spread, (sin(ang) * rad + spread) * 0.5f, 0.6f + rnd.nextFloat() * 1.6f, col.copy(alpha = 0.9f - t * 0.6f)))
        }
        list
    }
    Canvas(modifier) {
        val c = center
        val half = min(size.width, size.height) / 2f
        drawCircle(Brush.radialGradient(listOf(Color(0x88F9A8D4), Color(0x338B5CF6), Color.Transparent), c, half), half, c)
        rotate(-24f, c) {
            dots.forEach { drawCircle(it.color, it.r * density, Offset(c.x + it.x * half, c.y + it.y * half)) }
        }
        drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFFFBCFE8), Color.Transparent), c, half * 0.22f), half * 0.22f, c)
    }
}

/** Draws an occasional, short shooting star (700-1200 ms). */
@Composable
fun ShootingStar(modifier: Modifier = Modifier) {
    val reduced = rememberReducedMotion()
    val progress = remember { Animatable(0f) }
    var start by remember { mutableStateOf(Offset(0.7f, 0.1f)) }
    LaunchedEffect(reduced) {
        if (reduced) return@LaunchedEffect
        while (true) {
            delay(5_000L + Random.nextLong(0L, 8_000L))
            start = Offset(0.3f + Random.nextFloat() * 0.6f, Random.nextFloat() * 0.3f)
            progress.snapTo(0f)
            progress.animateTo(1f, tween(Random.nextInt(700, 1200), easing = LinearEasing))
            progress.snapTo(0f)
        }
    }
    Canvas(modifier.fillMaxSize()) {
        val p = progress.value
        if (p > 0f) {
            val travel = Offset(-size.width * 0.35f, size.height * 0.12f)
            val head = Offset(start.x * size.width, start.y * size.height) + travel * p
            val tail = head - travel * 0.35f
            drawLine(Brush.linearGradient(listOf(Color.Transparent, Color.White), tail, head), tail, head, strokeWidth = 2f * density, cap = StrokeCap.Round, alpha = 1f - p * 0.6f)
            drawCircle(Color.White.copy(alpha = 1f - p * 0.6f), 2.5f * density, head)
        }
    }
}

/** Connected stars; [lit] stars glow (used for unlocked achievements). */
@Composable
fun ConstellationDecoration(modifier: Modifier = Modifier, seed: Int = 1, lit: Boolean = true, nodes: Int = 5) {
    val points = remember(seed, nodes) {
        val rnd = Random(seed)
        List(nodes) { i -> Offset(0.1f + 0.8f * i / (nodes - 1).coerceAtLeast(1), 0.2f + 0.6f * rnd.nextFloat()) }
    }
    val line = if (lit) Color(0xFFA78BFA) else Color(0xFF817B9F)
    Canvas(modifier) {
        val pts = points.map { Offset(it.x * size.width, it.y * size.height) }
        for (i in 0 until pts.size - 1) {
            drawLine(line.copy(alpha = if (lit) 0.7f else 0.3f), pts[i], pts[i + 1], strokeWidth = density)
        }
        pts.forEach {
            if (lit) drawCircle(Color(0x66EC4899), density * 7f, it)
            drawCircle(if (lit) Color.White else line.copy(alpha = 0.6f), density * 2.5f, it)
        }
    }
}

// ---------------------------------------------------------------- logo

@Composable
fun GoEventLogo(modifier: Modifier = Modifier.size(110.dp), animate: Boolean = true) {
    val reduced = rememberReducedMotion()
    val transition = rememberInfiniteTransition(label = "logo")
    val spin by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing)), label = "orbit"
    )
    Canvas(modifier) {
        val c = center
        val r = size.minDimension * 0.28f
        drawCircle(
            Brush.radialGradient(listOf(Color(0x99EC4899), Color(0x558B5CF6), Color.Transparent), c, size.minDimension * 0.6f),
            size.minDimension * 0.6f, c
        )
        rotate(-20f + if (animate && !reduced) spin else 0f, c) {
            drawOval(Color(0xAAC4B5FD), Offset(c.x - r * 1.7f, c.y - r * 0.5f), Size(r * 3.4f, r), style = Stroke(density * 2f))
            drawCircle(Color(0xFF67E8F9), density * 4f, Offset(c.x + r * 1.7f, c.y))
        }
        drawCircle(
            Brush.linearGradient(listOf(Color(0xFF6D28D9), Color(0xFF8B5CF6), Color(0xFFEC4899)), Offset(c.x - r, c.y - r), Offset(c.x + r, c.y + r)),
            r, c
        )
        val star = Path().apply {
            moveTo(c.x, c.y - r * 0.7f)
            quadraticBezierTo(c.x, c.y, c.x + r * 0.7f, c.y)
            quadraticBezierTo(c.x, c.y, c.x, c.y + r * 0.7f)
            quadraticBezierTo(c.x, c.y, c.x - r * 0.7f, c.y)
            quadraticBezierTo(c.x, c.y, c.x, c.y - r * 0.7f)
            close()
        }
        drawPath(star, Color.White)
    }
}

// ---------------------------------------------------------------- full-screen background

@Composable
fun GalaxyBackground(
    variant: GalaxyVariant = GalaxyVariant.PLAIN,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val c = cosmic
    val soft = if (c.isDark) 1f else 0.55f
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(c.background, c.secondary, c.background)))
    ) {
        val purple = Color(0xFF6D28D9); val violet = Color(0xFF8B5CF6)
        val magenta = Color(0xFFC026D3); val pink = Color(0xFFEC4899); val blue = Color(0xFF2563EB)
        when (variant) {
            GalaxyVariant.HOME -> {
                NebulaGlow(Modifier.fillMaxSize(), listOf(purple, magenta, blue), Offset(0.2f, 0.05f), 0.7f, 0.3f)
                SpiralGalaxy(Modifier.size(220.dp).align(Alignment.TopEnd).offset(x = 70.dp, y = (-40).dp).alpha(0.8f * soft), seed = 11)
            }
            GalaxyVariant.AUTH -> {
                NebulaGlow(Modifier.fillMaxSize(), listOf(purple, pink, blue), Offset(0.5f, 0.35f), 0.8f, 0.38f)
                PlanetIllustration(Modifier.size(300.dp).align(Alignment.BottomStart).offset(x = (-110).dp, y = 90.dp).alpha(soft))
                MoonIllustration(Modifier.size(120.dp).align(Alignment.TopEnd).offset(x = 20.dp, y = 30.dp).alpha(soft), crescent = true)
            }
            GalaxyVariant.EXPLORE -> {
                NebulaGlow(Modifier.fillMaxSize(), listOf(blue, violet, purple), Offset(0.9f, 0.1f), 0.7f, 0.3f)
                PlanetIllustration(
                    Modifier.size(180.dp).align(Alignment.TopEnd).offset(x = 60.dp, y = (-30).dp).alpha(0.8f * soft),
                    colors = listOf(Color(0xFF7DD3FC), Color(0xFF2563EB), Color(0xFF1E1B4B)), hasRing = false
                )
            }
            GalaxyVariant.TICKETS -> {
                NebulaGlow(Modifier.fillMaxSize(), listOf(violet, pink), Offset(0.1f, 0.2f), 0.7f, 0.28f)
                PlanetIllustration(
                    Modifier.size(200.dp).align(Alignment.TopEnd).offset(x = 70.dp, y = (-40).dp).alpha(0.8f * soft),
                    colors = listOf(Color(0xFFF9A8D4), Color(0xFFDB2777), Color(0xFF500724))
                )
            }
            GalaxyVariant.SAVED -> {
                NebulaGlow(Modifier.fillMaxSize(), listOf(purple, blue), Offset(0.8f, 0.2f), 0.7f, 0.28f)
                MoonIllustration(Modifier.size(170.dp).align(Alignment.TopEnd).offset(x = 30.dp, y = (-10).dp).alpha(soft), crescent = true)
            }
            GalaxyVariant.POINTS -> {
                NebulaGlow(Modifier.fillMaxSize(), listOf(Color(0xFFEA580C), magenta, purple), Offset(0.85f, 0.1f), 0.7f, 0.25f)
                SunIllustration(Modifier.size(240.dp).align(Alignment.TopEnd).offset(x = 60.dp, y = (-50).dp).alpha(soft))
            }
            GalaxyVariant.PROFILE -> {
                NebulaGlow(Modifier.fillMaxSize(), listOf(violet, magenta, blue), Offset(0.5f, 0f), 0.8f, 0.3f)
                MoonIllustration(Modifier.size(140.dp).align(Alignment.TopStart).offset(x = (-30).dp, y = (-10).dp).alpha(soft))
            }
            GalaxyVariant.DETAILS -> NebulaGlow(Modifier.fillMaxSize(), listOf(purple, pink), Offset(0.5f, 0.1f), 0.7f, 0.22f)
            GalaxyVariant.PLAIN -> NebulaGlow(Modifier.fillMaxSize(), listOf(purple, blue), Offset(0.2f, 0.1f), 0.6f, 0.2f)
        }
        StarField(seed = variant.ordinal + 5)
        ShootingStar()
        content()
    }
}