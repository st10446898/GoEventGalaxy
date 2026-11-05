package com.goevent.ui.previews

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.goevent.ui.theme.GoEventTheme

@Preview(name = "Dark Theme Preview", showBackground = true, backgroundColor = 0xFF070616)
@Composable
fun PreviewDarkTheme() {
    GoEventTheme("Dark") {
        Text(
            text = "GoEvent Dark Theme",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Light Theme Preview", showBackground = true)
@Composable
fun PreviewLightTheme() {
    GoEventTheme("Light") {
        Text(
            text = "GoEvent Light Theme",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}
