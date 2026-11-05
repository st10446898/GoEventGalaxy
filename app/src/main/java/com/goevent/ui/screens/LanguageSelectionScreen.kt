package com.goevent.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goevent.ui.components.GalaxyScreen
import com.goevent.ui.components.GradientButton
import com.goevent.ui.galaxy.GalaxyVariant
import com.goevent.ui.i18n.tr

@Composable
fun LanguageSelectionScreen(
    currentLanguage: String,
    onNavigateBack: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    val languages = listOf("English", "Afrikaans", "isiXhosa")
    var selectedLanguage by remember { mutableStateOf(currentLanguage) }

    GalaxyScreen(GalaxyVariant.PLAIN, tr("Select Language"), onBack = onNavigateBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            languages.forEach { language ->
                val isSelected = selectedLanguage == language
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { selectedLanguage = language }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = language, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedLanguage = language }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            GradientButton(
                text = tr("Save Language Preference"),
                onClick = {
                    onLanguageSelected(selectedLanguage)
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
