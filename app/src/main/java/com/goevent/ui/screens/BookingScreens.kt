package com.goevent.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goevent.core.model.TicketDetail
import com.goevent.core.util.Formatters
import com.goevent.core.util.QRCodeGenerator
import com.goevent.core.util.TicketPdf
import com.goevent.ui.components.CosmicTag
import com.goevent.ui.components.EmptyView
import com.goevent.ui.components.ErrorText
import com.goevent.ui.components.ErrorView
import com.goevent.ui.components.EventImage
import com.goevent.ui.components.GalaxyScreen
import com.goevent.ui.components.GlassCard
import com.goevent.ui.components.GradientButton
import com.goevent.ui.components.LoadingView
import com.goevent.ui.components.OutlineButton
import com.goevent.ui.components.eventImageUrl
import com.goevent.ui.components.toast
import com.goevent.ui.galaxy.GalaxyVariant
import com.goevent.ui.theme.cosmic
import com.goevent.ui.viewmodel.SessionViewModel
import com.goevent.ui.viewmodel.TicketDetailViewModel
import com.goevent.ui.viewmodel.TicketsViewModel
import com.goevent.ui.viewmodel.appViewModel

// ===================================================================== 1. MY TICKETS

@Composable
fun MyTicketsScreen(session: SessionViewModel, onTicket: (String) -> Unit, onExplore: () -> Unit) {
    val vm = appViewModel { TicketsViewModel(it) }
    val s by vm.state.collectAsState()
    val ss by session.state.collectAsState()
    var showPast by remember { mutableStateOf(false) }
    LaunchedEffect(ss.profile?.id) { vm.load() }

    val upcoming = s.tickets.filter { Formatters.isUpcoming(it.bookings?.events?.eventDate ?: "") }
    val past = s.tickets.filterNot { Formatters.isUpcoming(it.bookings?.events?.eventDate ?: "") }
    val shown = if (showPast) past else upcoming

    GalaxyScreen(GalaxyVariant.TICKETS) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(8.dp))
            Text("My Tickets", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!showPast, { showPast = false }, { Text("Upcoming (${upcoming.size})") })
                FilterChip(showPast, { showPast = true }, { Text("Past (${past.size})") })
            }
        }
        when {
            s.loading && s.tickets.isEmpty() -> LoadingView()
            s.error != null && s.tickets.isEmpty() -> ErrorView(s.error.orEmpty(), vm::load)
            shown.isEmpty() -> EmptyView(
                if (showPast) "No past tickets" else "No upcoming tickets",
                if (showPast) "Tickets for events that have finished appear here." else "Book an event and your tickets will appear here.",
                action = if (showPast) null else ({ GradientButton("Discover events", onExplore) })
            )
            else -> LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items(shown, key = { it.id }) { t -> TicketCard(t) { onTicket(t.id) } }
            }
        }
    }
}

@Composable
private fun TicketCard(t: TicketDetail, onClick: () -> Unit) {
    val ev = t.bookings?.events
    GlassCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            EventImage(eventImageUrl(ev?.imageUrl), Modifier.size(72.dp).clip(RoundedCornerShape(16.dp)), null, ev?.title)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(ev?.title ?: "Event", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listOfNotNull(ev?.eventDate?.takeIf { it.isNotBlank() }?.let(Formatters::shortDate), ev?.venue?.takeIf { it.isNotBlank() }).joinToString(" · "),
                    color = cosmic.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CosmicTag(t.ticketType?.name?.ifBlank { "Ticket" } ?: "Ticket")
                    CosmicTag(t.status, color = if (t.status == "Valid") cosmic.success else cosmic.textMuted)
                }
            }
            Icon(Icons.Rounded.QrCode2, "Open ticket", tint = cosmic.violet, modifier = Modifier.size(32.dp))
        }
    }
}

// ===================================================================== 2. DIGITAL TICKET

@Composable
fun DigitalTicketScreen(ticketId: String, onBack: () -> Unit, onQr: () -> Unit) {
    val context = LocalContext.current
    val vm = appViewModel(key = "tk$ticketId") { TicketDetailViewModel(it, ticketId) }
    val s by vm.state.collectAsState()
    val t = s.ticket

    GalaxyScreen(GalaxyVariant.TICKETS, "Your ticket", onBack) {
        when {
            s.loading -> LoadingView()
            t == null -> ErrorView(s.error ?: "Ticket not found", vm::load)
            else -> {
                val ev = t.bookings?.events
                val qr = remember(t.qrToken) { QRCodeGenerator.generateQRCode(t.qrToken, 720, 720)?.asImageBitmap() }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    GlassCard(Modifier.fillMaxWidth(), 28.dp) {
                        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(ev?.title ?: "Event", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                            Text(listOfNotNull(ev?.eventDate?.takeIf { it.isNotBlank() }?.let(Formatters::date), ev?.startTime?.takeIf { it.isNotBlank() }?.let(Formatters::time)).joinToString(" · "),
                                color = cosmic.textSecondary, fontSize = 13.sp)
                            if (!ev?.venue.isNullOrBlank()) Text("${ev?.venue}, ${ev?.city}", color = cosmic.textSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
                            Box(Modifier.size(220.dp).clip(RoundedCornerShape(20.dp)).background(Color.White).padding(10.dp).clickable(onClick = onQr), contentAlignment = Alignment.Center) {
                                if (qr != null) Image(qr, "QR code for ticket ${t.ticketReference}", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                                else Text("QR unavailable", color = Color.Black)
                            }
                            Text("Tap the code to enlarge", color = cosmic.textMuted, fontSize = 12.sp)
                            Text(t.ticketReference, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = cosmic.violet)
                        }
                    }
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            PriceLine("Holder", t.holderName)
                            PriceLine("Type", t.ticketType?.name?.ifBlank { "Ticket" } ?: "Ticket")
                            PriceLine("Booking", t.bookings?.bookingReference ?: "")
                            PriceLine("Status", t.status, if (t.status == "Valid") cosmic.success else cosmic.warning)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlineButton("Download PDF", {
                            val file = TicketPdf.generate(context, t)
                            if (file != null) context.toast("PDF downloaded to Documents")
                            else context.toast("Failed to generate PDF")
                        }, Modifier.weight(1f), Icons.Rounded.Download)
                        OutlineButton("Share", {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Ticket for ${ev?.title}")
                                putExtra(Intent.EXTRA_TEXT, "Here is my ticket for ${ev?.title} (${t.ticketReference}). Show code at gate: ${t.qrToken}")
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Ticket"))
                        }, Modifier.weight(1f), Icons.Rounded.Share)
                    }
                    OutlineButton("Check ticket status", vm::verify, Modifier.fillMaxWidth(), Icons.Rounded.CheckCircle, enabled = !s.checking)
                    s.validation?.let { v ->
                        GlassCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Server result: ${v.status}", fontWeight = FontWeight.Bold, color = if (v.status.equals("VALID", true)) cosmic.success else cosmic.warning)
                                Text("${v.eventTitle} · ${v.holderName}", color = cosmic.textSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                    s.validationError?.let { ErrorText(it) }
                }
            }
        }
    }
}

// ===================================================================== 3. FULL-SCREEN QR

@Composable
fun QrViewScreen(ticketId: String, onBack: () -> Unit) {
    val vm = appViewModel(key = "tk$ticketId") { TicketDetailViewModel(it, ticketId) }
    val s by vm.state.collectAsState()
    val t = s.ticket
    GalaxyScreen(GalaxyVariant.PLAIN, "Scan at entry", onBack) {
        when {
            s.loading -> LoadingView()
            t == null -> ErrorView(s.error ?: "Ticket not found", vm::load)
            else -> {
                val qr = remember(t.qrToken) { QRCodeGenerator.generateQRCode(t.qrToken, 1000, 1000)?.asImageBitmap() }
                Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White).padding(16.dp), contentAlignment = Alignment.Center) {
                        if (qr != null) Image(qr, "QR code for ticket ${t.ticketReference}", Modifier.fillMaxWidth(), contentScale = ContentScale.Fit)
                        else Text("QR unavailable", color = Color.Black)
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(t.ticketReference, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(t.holderName, color = cosmic.textSecondary)
                    Spacer(Modifier.height(8.dp))
                    Text("Turn your screen brightness up for easier scanning.", color = cosmic.textMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun PriceLine(label: String, value: String, color: Color = Color.Unspecified, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = cosmic.textSecondary, fontSize = if (bold) 16.sp else 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = color, fontSize = if (bold) 20.sp else 14.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium)
    }
}
