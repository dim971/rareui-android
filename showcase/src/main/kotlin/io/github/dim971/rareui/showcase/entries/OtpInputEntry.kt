package io.github.dim971.rareui.showcase.entries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import io.github.dim971.rareui.components.inputs.OtpCharacterSet
import io.github.dim971.rareui.components.inputs.OtpInput
import io.github.dim971.rareui.components.inputs.OtpSize
import io.github.dim971.rareui.components.inputs.OtpStatus
import io.github.dim971.rareui.showcase.CatalogEntry
import io.github.dim971.rareui.showcase.Demo

val otpInputEntry: CatalogEntry =
    CatalogEntry(
        name = "OTP Input",
        summary = "A row of boxes for a one time code, with characters that roll in and a caret that slides.",
        demos =
            listOf(
                Demo(
                    title = "Type a code",
                    note =
                        "Characters roll in from below. Deleting one sends it back down the way " +
                            "it came; replacing one pushes it up and out of the top, so the two " +
                            "never look alike.",
                    code =
                        "OtpInput(code = code, onCodeChange = { code = it }, onComplete = ::verify)",
                ) { OtpTypingDemo() },
                Demo(
                    title = "Accepted and refused",
                    note =
                        "Success draws each box its own outline, a twentieth of a second apart, " +
                            "so the green runs along the row. An error shakes the row once and " +
                            "turns it red.",
                    code =
                        "OtpInput(code = code, onCodeChange = { code = it }, status = OtpStatus.SUCCESS)\n" +
                            "OtpInput(code = code, onCodeChange = { code = it }, status = OtpStatus.ERROR)",
                ) { OtpStatusDemo() },
                Demo(
                    title = "Sizes and character sets",
                    code =
                        "OtpInput(code = code, onCodeChange = { code = it }, length = 4,\n" +
                            "    characterSet = OtpCharacterSet.ALPHANUMERIC, size = OtpSize.LARGE)\n" +
                            "OtpInput(code = code, onCodeChange = { code = it }, length = 4,\n" +
                            "    size = OtpSize.SMALL, mask = true)",
                ) { OtpVariantsDemo() },
            ),
    ) {
        OtpInput(code = "42", onCodeChange = {}, length = 3, size = OtpSize.SMALL)
    }

@Composable
private fun OtpTypingDemo() {
    var code by remember { mutableStateOf("") }
    var completed by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OtpInput(code = code, onCodeChange = { code = it }, onComplete = { completed = it })
        Text(
            text = completed?.let { "Completed: $it" } ?: "Waiting for six digits",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OtpStatusDemo() {
    var code by remember { mutableStateOf("123456") }
    var status by remember { mutableStateOf(OtpStatus.SUCCESS) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        OtpInput(code = code, onCodeChange = { code = it }, status = status)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OtpStatus.entries.forEach { candidate ->
                FilterChip(
                    selected = candidate == status,
                    onClick = { status = candidate },
                    label = { Text(candidate.name.lowercase().replaceFirstChar { it.uppercase() }) },
                )
            }
        }
    }
}

@Composable
private fun OtpVariantsDemo() {
    var letters by remember { mutableStateOf("AB") }
    var masked by remember { mutableStateOf("12") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OtpInput(
            code = letters,
            onCodeChange = { letters = it },
            length = 4,
            characterSet = OtpCharacterSet.ALPHANUMERIC,
            size = OtpSize.LARGE,
        )
        OtpInput(
            code = masked,
            onCodeChange = { masked = it },
            length = 4,
            size = OtpSize.SMALL,
            mask = true,
        )
    }
}
