package com.selfuse.timesquest.ui.parent

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.selfuse.timesquest.ui.theme.QuestColors
import kotlin.random.Random

/**
 * Carry-addition parent gate (e.g. 38 + 47 = ?) to prevent accidental kid access.
 */
@Composable
fun ParentGateDialog(
    title: String = "家长守护验证",
    subtitle: String = "请家长算一下这道题，验证后进入设置：",
    onVerified: () -> Unit,
    onDismiss: () -> Unit
) {
    val pair = remember {
        val a = Random.nextInt(26, 59)
        val b = Random.nextInt(27, 49)
        a to b
    }
    var answerText by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = QuestColors.InkSecondary)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "${pair.first} + ${pair.second} = ?",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = QuestColors.SkyDark
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = answerText,
                    onValueChange = {
                        answerText = it.filter { ch -> ch.isDigit() }.take(3)
                        errorMsg = null
                    },
                    label = { Text("输入答案") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(errorMsg!!, fontSize = 12.sp, color = QuestColors.DangerRed)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (answerText.toIntOrNull() == pair.first + pair.second) {
                    onVerified()
                } else {
                    errorMsg = "答案不太对哦，请再算一下"
                }
            }) {
                Text("确认进入", fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
