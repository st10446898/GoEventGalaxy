package com.goevent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goevent.GoEventApp
import com.goevent.data.offline.ConnectivityMonitor
import com.goevent.ui.i18n.tr
import com.goevent.ui.theme.cosmic

/** Shows a strip at the top when offline, or while changes are waiting to sync. Hidden otherwise. */
@Composable
fun OfflineBanner() {
    val app = LocalContext.current.applicationContext as GoEventApp
    val online by ConnectivityMonitor.isOnline.collectAsState()
    val pending by app.container.sync.pendingCount.collectAsState(initial = 0)
    val status by app.container.sync.status.collectAsState()

    val text = when {
        !online && pending == 1 -> tr("Offline. 1 change saved and will sync when you reconnect.")
        !online && pending > 1 -> tr("Offline. {0} changes saved and will sync when you reconnect.", pending)
        !online -> tr("Offline. Showing your saved data.")
        status.syncing -> tr("Syncing your changes…")
        pending == 1 -> tr("1 change waiting to sync.")
        pending > 1 -> tr("{0} changes waiting to sync.", pending)
        else -> return
    }

    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(cosmic.elevated)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (online) Icons.Rounded.Sync else Icons.Rounded.CloudOff,
            contentDescription = null,
            tint = if (online) cosmic.violet else cosmic.warning
        )
        Text(text, Modifier.padding(start = 8.dp), color = cosmic.textMain, fontSize = 13.sp)
    }
}