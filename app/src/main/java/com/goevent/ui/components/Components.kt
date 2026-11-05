package com.goevent.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.goevent.BuildConfig
import com.goevent.core.model.Event
import com.goevent.core.util.Formatters
import com.goevent.core.util.ImageUrlResolver
import com.goevent.ui.galaxy.GalaxyBackground
import com.goevent.ui.galaxy.GalaxyVariant
import com.goevent.ui.galaxy.MoonIllustration
import com.goevent.ui.i18n.tr
import com.goevent.ui.i18n.trPlain
import com.goevent.ui.theme.Galaxy
import com.goevent.ui.theme.cosmic

/** Shows a short message. The text is translated to the chosen language. */
fun Context.toast(message: String) = Toast.makeText(this, trPlain(message), Toast.LENGTH_SHORT).show()

fun categoryIcon(name: String?): ImageVector = when (name) {
    "MusicNote" -> Icons.Rounded.MusicNote
    "Laptop" -> Icons.Rounded.Laptop
    "SportsSoccer" -> Icons.Rounded.SportsSoccer
    "Brush" -> Icons.Rounded.Brush
    "Restaurant" -> Icons.Rounded.Restaurant
    "TheaterComedy" -> Icons.Rounded.TheaterComedy
    "RocketLaunch" -> Icons.Rounded.RocketLaunch
    "Public" -> Icons.Rounded.Public
    "Diamond" -> Icons.Rounded.Diamond
    else -> Icons.Rounded.Star
}

fun eventImageUrl(raw: String?): String? = ImageUrlResolver.event(raw, BuildConfig.SUPABASE_URL)
fun rewardImageUrl(raw: String?): String? = ImageUrlResolver.reward(raw, BuildConfig.SUPABASE_URL)

// ------------------------------------------------------------------ screen frame

/**
 * Rule for this file: text passed IN to these components is an English key.
 * The component translates it with tr(). Event and user data (titles, names) are never translated.
 */
@Composable
fun GalaxyScreen(
    variant: GalaxyVariant = GalaxyVariant.PLAIN,
    title: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    GalaxyBackground(variant) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            if (title != null || onBack != null) TopBar(title.orEmpty(), onBack, actions)
            content()
        }
    }
}

@Composable
fun TopBar(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(56.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconButton(onBack, Modifier.size(48.dp)) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Back")) }
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Text(tr(title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        actions()
    }
}

// ------------------------------------------------------------------ buttons

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(120), label = "press")
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .fillMaxWidth().height(52.dp).scale(scale).clip(shape)
            .background(if (enabled) Galaxy.galaxy else SolidColor(cosmic.border), shape)
            .clickable(interaction, null, enabled = enabled && !loading, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (icon != null) Icon(icon, null, Modifier.size(20.dp), tint = Color.White)
                Text(tr(text), color = Color.White.copy(alpha = if (enabled) 1f else 0.6f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier.fillMaxWidth().height(48.dp).clip(shape)
            .border(BorderStroke(1.dp, if (enabled) cosmic.primary else cosmic.border), shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) Icon(icon, null, Modifier.size(20.dp), tint = cosmic.primary)
            Text(tr(text), color = if (enabled) cosmic.primary else cosmic.textMuted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ------------------------------------------------------------------ inputs

@Composable
fun GalaxyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    error: String? = null,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    singleLine: Boolean = true
) {
    val c = cosmic
    val focus = LocalFocusManager.current
    var visible by remember { mutableStateOf(false) }
    val leading: (@Composable () -> Unit)? = leadingIcon?.let { icon -> { Icon(icon, null, Modifier.size(20.dp)) } }
    val trailing: (@Composable () -> Unit)? = if (isPassword) ({
        IconButton({ visible = !visible }) {
            Icon(
                if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                if (visible) tr("Hide password") else tr("Show password")
            )
        }
    }) else null
    val translatedError = error?.let { tr(it) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        label = { Text(tr(label), fontSize = 12.sp) },
        leadingIcon = leading,
        trailingIcon = trailing,
        isError = error != null,
        supportingText = translatedError?.let { msg -> { Text(msg, fontSize = 12.sp) } },
        singleLine = singleLine,
        textStyle = MaterialTheme.typography.bodyLarge,
        visualTransformation = if (isPassword && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onNext = { focus.moveFocus(FocusDirection.Down) },
            onDone = { focus.clearFocus(); onImeAction?.invoke() },
            onSearch = { focus.clearFocus(); onImeAction?.invoke() }
        ),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = c.violet, unfocusedBorderColor = c.border, errorBorderColor = c.error,
            focusedContainerColor = c.card, unfocusedContainerColor = c.card, errorContainerColor = c.card,
            focusedTextColor = c.textMain, unfocusedTextColor = c.textMain,
            cursorColor = c.violet, focusedLabelColor = c.violet, unfocusedLabelColor = c.textSecondary,
            focusedLeadingIconColor = c.violet, unfocusedLeadingIconColor = c.textSecondary,
            errorSupportingTextColor = c.error
        )
    )
}

// ------------------------------------------------------------------ cards & text

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    radius: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(radius)
    val base = modifier.clip(shape).background(cosmic.card.copy(alpha = 0.94f), shape).border(1.dp, cosmic.border, shape)
    Column(if (onClick != null) base.clickable(role = Role.Button, onClick = onClick) else base, content = content)
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(tr(title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
        if (actionLabel != null && onAction != null) {
            Text(
                tr(actionLabel), color = cosmic.violet, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.heightIn(min = 48.dp).clickable(onClick = onAction).padding(horizontal = 4.dp).wrapCenter()
            )
        }
    }
}

private fun Modifier.wrapCenter(): Modifier = this.then(Modifier.padding(top = 12.dp))

@Composable
fun CosmicTag(text: String, modifier: Modifier = Modifier, color: Color = cosmic.primary) {
    Text(
        text, modifier = modifier.clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.2f)).padding(horizontal = 10.dp, vertical = 4.dp),
        color = if (cosmic.isDark) Color.White else color, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1
    )
}

@Composable
fun ErrorText(message: String, modifier: Modifier = Modifier) {
    Text(tr(message), modifier, color = cosmic.error, fontSize = 13.sp)
}

// ------------------------------------------------------------------ state views

@Composable
fun LoadingView(modifier: Modifier = Modifier.fillMaxSize()) {
    Box(modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator(color = cosmic.violet) }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier.fillMaxSize()) {
    Column(modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(tr("Lost in space"), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(tr(message), color = cosmic.textSecondary, textAlign = TextAlign.Center, fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))
        GradientButton("Try again", onRetry, Modifier.fillMaxWidth())
    }
}

@Composable
fun EmptyView(title: String, message: String, modifier: Modifier = Modifier.fillMaxSize(), action: (@Composable () -> Unit)? = null) {
    Column(modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        MoonIllustration(Modifier.size(150.dp), crescent = true)
        Text(tr(title), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(tr(message), color = cosmic.textSecondary, textAlign = TextAlign.Center, fontSize = 14.sp)
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            action()
        }
    }
}

// ------------------------------------------------------------------ images & event cards

@Composable
fun EventImage(url: String?, modifier: Modifier = Modifier, categoryIconName: String? = null, contentDescription: String? = null) {
    Box(modifier.background(Galaxy.nebula)) {
        Icon(categoryIcon(categoryIconName), null, Modifier.align(Alignment.Center).size(40.dp), tint = Color.White.copy(alpha = 0.35f))
        if (url != null) {
            AsyncImage(model = url, contentDescription = contentDescription, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
fun FavouriteButton(isFav: Boolean, title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick, modifier.size(48.dp),
        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.35f))
    ) {
        Icon(
            if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            if (isFav) tr("Remove {0} from saved", title) else tr("Save {0}", title),
            tint = if (isFav) cosmic.pink else Color.White
        )
    }
}

@Composable
fun EventCard(
    event: Event,
    categoryName: String?,
    isFav: Boolean,
    onClick: () -> Unit,
    onFav: () -> Unit,
    modifier: Modifier = Modifier,
    imageHeight: Dp? = null
) {
    GlassCard(modifier, 20.dp, onClick) {
        Column(Modifier.padding(14.dp)) {
            Box {
                val size = if (imageHeight != null) Modifier.height(imageHeight) else Modifier.aspectRatio(4f / 3f)
                EventImage(eventImageUrl(event.imageUrl), Modifier.fillMaxWidth().then(size).clip(RoundedCornerShape(14.dp)), null, event.title)
                FavouriteButton(isFav, event.title, onFav, Modifier.align(Alignment.TopEnd).padding(4.dp))
                if (categoryName != null) CosmicTag(categoryName, Modifier.align(Alignment.BottomStart).padding(8.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(event.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CalendarMonth, null, Modifier.size(14.dp), tint = cosmic.textMuted)
                Spacer(Modifier.width(4.dp))
                Text("${Formatters.shortDate(event.eventDate)} · ${event.city}", color = cosmic.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (event.rating != null && event.rating > 0.0) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Star, null, Modifier.size(13.dp), tint = Color(0xFFFFC107))
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "%.1f (%d)".format(event.rating, event.ratingCount),
                        color = cosmic.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (event.startingPriceCents == 0) tr("Free") else tr("From {0}", Formatters.rand(event.startingPriceCents)),
                color = cosmic.violet, fontSize = 14.sp, fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun EventRow(event: Event, isFav: Boolean, onClick: () -> Unit, onFav: () -> Unit, modifier: Modifier = Modifier) {
    GlassCard(modifier, 20.dp, onClick) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val (day, month) = Formatters.dayAndMonth(event.eventDate)
            Column(
                Modifier.size(width = 56.dp, height = 72.dp).clip(RoundedCornerShape(14.dp)).background(Galaxy.galaxy),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
            ) {
                Text(day, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(month, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(event.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${Formatters.time(event.startTime)} · ${event.venue}", color = cosmic.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(event.city, color = cosmic.textMuted, fontSize = 12.sp)
                if (event.rating != null && event.rating > 0.0) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Star, null, Modifier.size(13.dp), tint = Color(0xFFFFC107))
                        Spacer(Modifier.width(3.dp))
                        Text(
                            "%.1f (%d)".format(event.rating, event.ratingCount),
                            color = cosmic.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
                Text(
                    if (event.startingPriceCents == 0) tr("Free") else tr("From {0}", Formatters.rand(event.startingPriceCents)),
                    color = cosmic.violet, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                )
            }
            IconButton(onFav, Modifier.size(48.dp)) {
                Icon(
                    if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    if (isFav) tr("Remove {0} from saved", event.title) else tr("Save {0}", event.title),
                    tint = if (isFav) cosmic.pink else cosmic.textSecondary
                )
            }
        }
    }
}

@Composable
fun CategoryBubble(name: String, iconName: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.width(76.dp).clip(RoundedCornerShape(16.dp)).clickable(role = Role.Button, onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(64.dp).clip(CircleShape)
                .background(if (selected) Galaxy.galaxy else SolidColor(cosmic.card))
                .border(1.dp, if (selected) Color.Transparent else cosmic.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(categoryIcon(iconName), name, Modifier.size(28.dp), tint = if (selected) Color.White else cosmic.violet)
        }
        Spacer(Modifier.height(6.dp))
        Text(name, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (selected) cosmic.textMain else cosmic.textSecondary)
    }
}

/** Title and subtitle are passed in already translated when they are fixed labels. Data is shown as-is. */
@Composable
fun InfoRow(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(cosmic.primary.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(22.dp), tint = cosmic.violet)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = cosmic.textSecondary, fontSize = 13.sp)
        }
    }
}

// ------------------------------------------------------------------ bottom navigation

data class NavItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun GalaxyBottomBar(currentRoute: String?, items: List<NavItem>, onSelect: (NavItem) -> Unit) {
    val c = cosmic
    val shape = RoundedCornerShape(24.dp)
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(
            Modifier.fillMaxWidth().height(76.dp).clip(shape).background(c.card.copy(alpha = 0.96f), shape).border(1.dp, c.border, shape).selectableGroup(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                val pill by animateFloatAsState(if (selected) 1f else 0f, tween(180), label = "pill")
                val label = tr(item.label)
                Column(
                    Modifier.weight(1f).fillMaxHeight().selectable(selected, onClick = { onSelect(item) }, role = Role.Tab),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        Modifier.size(width = 52.dp, height = 32.dp).clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(c.primary.copy(alpha = 0.35f * pill), c.pink.copy(alpha = 0.2f * pill)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(item.icon, label, Modifier.size(23.dp), tint = if (selected) c.violet else c.textMuted)
                    }
                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = if (selected) c.violet else c.textMuted)
                }
            }
        }
    }
}