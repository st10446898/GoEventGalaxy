package com.goevent.ui.screens.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goevent.GoEventApp
import com.goevent.core.model.Event
import com.goevent.core.model.TicketType
import com.goevent.core.util.Formatters
import com.goevent.core.util.PricingCalculator
import com.goevent.data.SupabaseClientManager
import com.goevent.ui.components.CosmicTag
import com.goevent.ui.components.ErrorView
import com.goevent.ui.components.GalaxyScreen
import com.goevent.ui.components.GlassCard
import com.goevent.ui.components.GradientButton
import com.goevent.ui.components.InfoRow
import com.goevent.ui.components.LoadingView
import com.goevent.ui.components.OutlineButton
import com.goevent.ui.galaxy.GalaxyBackground
import com.goevent.ui.galaxy.GalaxyVariant
import com.goevent.ui.theme.Galaxy
import com.goevent.ui.theme.cosmic
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class BookingStep { SELECT_TICKETS, CHECKOUT, PAYMENT, CONFIRMATION }

@Composable
fun BookingFlowScreen(
    eventId: String,
    onExit: () -> Unit,
    onViewTickets: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val container = (context.applicationContext as GoEventApp).container
    val client = SupabaseClientManager.client
    val bookingRepo = container.bookingRepository
    val eventRepo = container.eventRepository
    val profileRepo = container.profileRepository

    var step by remember { mutableStateOf(BookingStep.SELECT_TICKETS) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var event by remember { mutableStateOf<Event?>(null) }
    var ticketTypes by remember { mutableStateOf<List<TicketType>>(emptyList()) }
    var profileBalance by remember { mutableIntStateOf(0) }

    var selectedTypeId by remember { mutableStateOf<String?>(null) }
    var quantity by remember { mutableIntStateOf(1) }
    var pointsToRedeem by remember { mutableIntStateOf(0) }

    var bookingId by remember { mutableStateOf("") }
    var bookingRef by remember { mutableStateOf("") }
    var finalAmountCents by remember { mutableIntStateOf(0) }
    var pointsEarned by remember { mutableIntStateOf(0) }
    var isBusy by remember { mutableStateOf(false) }

    LaunchedEffect(eventId) {
        loading = true
        try {
            val evResult = eventRepo.fetchEventById(eventId)
            event = evResult.getOrNull()

            val types = withContext(Dispatchers.IO) {
                client.postgrest["ticket_types"]
                    .select { filter { eq("event_id", eventId) } }
                    .decodeList<TicketType>()
            }
            ticketTypes = types
            if (types.isNotEmpty()) selectedTypeId = types.first().id

            val currentUserId = client.auth.currentUserOrNull()?.id
            if (currentUserId != null) {
                val profResult = profileRepo.fetchProfile(currentUserId)
                profResult.getOrNull()?.let { profileBalance = it.gopointsBalance }
            }
        } catch (e: Exception) {
            error = e.localizedMessage ?: "Failed to load event details"
        } finally {
            loading = false
        }
    }

    val selectedType = ticketTypes.firstOrNull { it.id == selectedTypeId }

    GalaxyScreen(GalaxyVariant.DETAILS, when(step) {
        BookingStep.SELECT_TICKETS -> "Choose Tickets"
        BookingStep.CHECKOUT -> "Checkout"
        BookingStep.PAYMENT -> "Payment"
        BookingStep.CONFIRMATION -> "Booking Confirmed"
    }, onBack = {
        when (step) {
            BookingStep.SELECT_TICKETS -> onExit()
            BookingStep.CHECKOUT -> step = BookingStep.SELECT_TICKETS
            BookingStep.PAYMENT -> step = BookingStep.CHECKOUT
            BookingStep.CONFIRMATION -> onExit()
        }
    }) {
        when {
            loading -> LoadingView()
            error != null -> ErrorView(error!!, onRetry = {})
            event == null -> ErrorView("Event not found", onRetry = { onExit() })
            else -> {
                when (step) {
                    BookingStep.SELECT_TICKETS -> {
                        Column(
                            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(event!!.title, style = MaterialTheme.typography.titleLarge)
                            Text("${Formatters.date(event!!.eventDate)} · ${event!!.venue}", color = cosmic.textSecondary, fontSize = 13.sp)
                            if (ticketTypes.isEmpty()) {
                                Text("Tickets are not on sale yet.", color = cosmic.textSecondary)
                            }
                            ticketTypes.forEach { t ->
                                val soldOut = t.availableQuantity <= 0
                                val isSelected = t.id == selectedTypeId
                                GlassCard(
                                    Modifier.fillMaxWidth().border(if (isSelected) 2.dp else 0.dp, if (isSelected) cosmic.violet else Color.Transparent, RoundedCornerShape(20.dp)),
                                    onClick = if (soldOut) null else {
                                        {
                                            selectedTypeId = t.id
                                            quantity = PricingCalculator.clampQuantity(quantity, t.availableQuantity)
                                        }
                                    }
                                ) {
                                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(t.name, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                when {
                                                    soldOut -> "Sold out"
                                                    t.availableQuantity <= 10 -> "Only ${t.availableQuantity} left"
                                                    else -> "${t.availableQuantity} available"
                                                },
                                                color = if (soldOut) cosmic.error else if (t.availableQuantity <= 10) cosmic.warning else cosmic.textSecondary,
                                                fontSize = 12.sp
                                            )
                                        }
                                        Text(if (t.priceCents == 0) "Free" else Formatters.rand(t.priceCents), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = cosmic.violet)
                                    }
                                }
                            }
                            if (selectedType != null) {
                                val max = PricingCalculator.maxQuantity(selectedType!!.availableQuantity)
                                GlassCard(Modifier.fillMaxWidth()) {
                                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text("Quantity", style = MaterialTheme.typography.titleMedium)
                                            Text("Up to $max per booking", color = cosmic.textSecondary, fontSize = 12.sp)
                                        }
                                        IconButton({ quantity = (quantity - 1).coerceAtLeast(1) }, Modifier.size(48.dp), enabled = quantity > 1) {
                                            Icon(Icons.Rounded.Remove, "Fewer tickets")
                                        }
                                        Text("$quantity", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                                        IconButton({ quantity = (quantity + 1).coerceAtMost(max) }, Modifier.size(48.dp), enabled = quantity < max) {
                                            Icon(Icons.Rounded.Add, "More tickets")
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        Column(Modifier.fillMaxWidth().background(cosmic.card).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                            if (selectedType != null) {
                                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                    Text("Subtotal", color = cosmic.textSecondary, modifier = Modifier.weight(1f))
                                    Text(Formatters.rand(PricingCalculator.subtotal(selectedType!!.priceCents, quantity)), fontWeight = FontWeight.Bold)
                                }
                            }
                            GradientButton("Continue", {
                                if (selectedType != null) step = BookingStep.CHECKOUT
                            }, Modifier.fillMaxWidth(), enabled = selectedType != null)
                        }
                    }

                    BookingStep.CHECKOUT -> {
                        val subtotal = PricingCalculator.subtotal(selectedType!!.priceCents, quantity)
                        val maxPoints = PricingCalculator.maxRedeemablePoints(profileBalance, subtotal)
                        val quote = PricingCalculator.quote(selectedType!!.priceCents, quantity, pointsToRedeem, profileBalance)

                        Column(
                            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            GlassCard(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(event!!.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    InfoRow(Icons.Rounded.CalendarMonth, Formatters.date(event!!.eventDate), "${Formatters.time(event!!.startTime)} – ${Formatters.time(event!!.endTime)}")
                                    InfoRow(Icons.Rounded.Place, event!!.venue, event!!.city)
                                    InfoRow(Icons.Rounded.ConfirmationNumber, "$quantity × ${selectedType!!.name}", if (selectedType!!.priceCents == 0) "Free" else Formatters.rand(selectedType!!.priceCents) + " each")
                                }
                            }
                            if (maxPoints > 0) {
                                GlassCard(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.Star, null, tint = cosmic.warning)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Use GoPoints", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                            Text("$profileBalance available", color = cosmic.textSecondary, fontSize = 12.sp)
                                        }
                                        Slider(
                                            value = pointsToRedeem.toFloat(),
                                            onValueChange = { pointsToRedeem = it.toInt() },
                                            valueRange = 0f..maxPoints.toFloat(),
                                            colors = SliderDefaults.colors(thumbColor = cosmic.violet, activeTrackColor = cosmic.violet)
                                        )
                                        Text(
                                            if (pointsToRedeem == 0) "500 points = R50 off" else "$pointsToRedeem points = ${Formatters.rand(quote.discountCents)} off",
                                            color = cosmic.textSecondary, fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                            GlassCard(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CheckoutPriceLine("Subtotal", Formatters.rand(quote.subtotalCents))
                                    if (quote.discountCents > 0) CheckoutPriceLine("GoPoints discount", "−" + Formatters.rand(quote.discountCents), cosmic.success)
                                    CheckoutPriceLine("Total", if (quote.isFree) "Free" else Formatters.rand(quote.totalCents), bold = true)
                                    val earn = PricingCalculator.estimatedPointsEarned(quote.totalCents)
                                    if (earn > 0) Text("You will earn about $earn GoPoints", color = cosmic.violet, fontSize = 12.sp)
                                }
                            }
                        }
                        Box(Modifier.fillMaxWidth().background(cosmic.card).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                            GradientButton(
                                if (quote.isFree) "Confirm Free Booking" else "Proceed to Payment",
                                {
                                    isBusy = true
                                    scope.launch {
                                        val res = bookingRepo.createBooking(event!!.id, selectedType!!.id, quantity, pointsToRedeem)
                                        isBusy = false
                                        val bookingRes = res.getOrNull()
                                        if (bookingRes != null) {
                                            bookingId = bookingRes.bookingId
                                            bookingRef = bookingRes.bookingReference
                                            finalAmountCents = bookingRes.finalAmountCents
                                            if (bookingRes.isFree) {
                                                val vRes = bookingRepo.verifySimulatedPayment(bookingId, "SUCCESS")
                                                vRes.getOrNull()?.let { pointsEarned = it.pointsEarned }
                                                step = BookingStep.CONFIRMATION
                                            } else {
                                                step = BookingStep.PAYMENT
                                            }
                                        } else {
                                            error = res.exceptionOrNull()?.localizedMessage ?: "Failed to create booking"
                                        }
                                    }
                                },
                                Modifier.fillMaxWidth(),
                                loading = isBusy
                            )
                        }
                    }

                    BookingStep.PAYMENT -> {
                        Column(
                            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            GlassCard(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Amount Due", color = cosmic.textSecondary, fontSize = 13.sp)
                                    Text(Formatters.rand(finalAmountCents), fontSize = 36.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(4.dp))
                                    Text("Booking $bookingRef", color = cosmic.textMuted, fontSize = 12.sp)
                                }
                            }
                            GlassCard(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CosmicTag("Secure Payment")
                                    Text(
                                        "Complete your payment to confirm your booking and issue your digital ticket QR codes.",
                                        color = cosmic.textSecondary, fontSize = 14.sp
                                    )
                                    Text("Your seats are held securely.", color = cosmic.warning, fontSize = 12.sp)
                                }
                            }
                        }
                        Column(
                            Modifier.fillMaxWidth().background(cosmic.card).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GradientButton("Pay ${Formatters.rand(finalAmountCents)}", {
                                isBusy = true
                                scope.launch {
                                    val vRes = bookingRepo.verifySimulatedPayment(bookingId, "SUCCESS")
                                    isBusy = false
                                    val verification = vRes.getOrNull()
                                    if (verification != null) {
                                        pointsEarned = verification.pointsEarned
                                        step = BookingStep.CONFIRMATION
                                    } else {
                                        error = vRes.exceptionOrNull()?.localizedMessage ?: "Payment verification failed"
                                    }
                                }
                            }, Modifier.fillMaxWidth(), loading = isBusy)

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlineButton("Cancel", {
                                    scope.launch {
                                        bookingRepo.verifySimulatedPayment(bookingId, "CANCELLED")
                                        onExit()
                                    }
                                }, Modifier.weight(1f))
                            }
                        }
                    }

                    BookingStep.CONFIRMATION -> {
                        GalaxyBackground(GalaxyVariant.POINTS) {
                            Column(
                                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                            ) {
                                Box(Modifier.size(96.dp).clip(CircleShape).background(Galaxy.galaxy), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.CheckCircle, null, Modifier.size(56.dp), tint = Color.White)
                                }
                                Spacer(Modifier.height(20.dp))
                                Text("Booking Confirmed", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                                Text("You're all set for the event.", color = cosmic.textSecondary)
                                Spacer(Modifier.height(24.dp))
                                GlassCard(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(event!!.title, style = MaterialTheme.typography.titleLarge)
                                        Text("${Formatters.date(event!!.eventDate)} · ${event!!.venue}", color = cosmic.textSecondary, fontSize = 13.sp)
                                        CheckoutPriceLine("Booking reference", bookingRef, bold = false)
                                        CheckoutPriceLine("Tickets", "$quantity")
                                        CheckoutPriceLine("Paid", if (finalAmountCents == 0) "Free" else Formatters.rand(finalAmountCents))
                                        if (pointsEarned > 0) CheckoutPriceLine("GoPoints earned", "+$pointsEarned", cosmic.success)
                                    }
                                }
                                Spacer(Modifier.height(24.dp))
                                GradientButton("View My Tickets", onViewTickets, Modifier.fillMaxWidth(), icon = Icons.Rounded.ConfirmationNumber)
                                Spacer(Modifier.height(10.dp))
                                OutlineButton("Back to Home", onExit, Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckoutPriceLine(label: String, value: String, color: Color = Color.Unspecified, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = cosmic.textSecondary, fontSize = if (bold) 16.sp else 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = color, fontSize = if (bold) 20.sp else 14.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium)
    }
}
