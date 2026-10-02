package com.example.tasktracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tasktracker.ui.theme.extraColors

@Composable
fun PasswordResetScreen(
    initialEmail: String,
    isSyncing: Boolean,
    authError: String?,
    infoMessage: String?,
    onSendReset: (String) -> Unit,
    onBackToSignIn: () -> Unit
) {
    val colors = MaterialTheme.extraColors
    var emailInput by remember { mutableStateOf(initialEmail) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Reset Password",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = colors.ink
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Enter your registered email address below. We will send you a link to reset your password.",
            fontSize = 14.sp,
            color = colors.muted
        )

        Spacer(Modifier.height(20.dp))

        // Info / Success Notice
        infoMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.accentSoft)
                    .border(1.dp, colors.accent, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text(msg, color = colors.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(14.dp))
        }

        // Error Notice
        authError?.let { err ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.high.copy(alpha = 0.15f))
                    .border(1.dp, colors.high, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text(err, color = colors.high, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(14.dp))
        }

        OutlinedTextField(
            value = emailInput,
            onValueChange = { emailInput = it },
            label = { Text("Email Address") },
            singleLine = true,
            colors = textFieldColors(),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                onSendReset(emailInput.trim())
            },
            enabled = !isSyncing,
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.paper),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Send Reset Link", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        if (isSyncing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBackToSignIn,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.ink),
            border = BorderStroke(1.dp, colors.line),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Back to Sign In", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.extraColors.accent,
    unfocusedBorderColor = MaterialTheme.extraColors.line,
    focusedContainerColor = MaterialTheme.extraColors.card,
    unfocusedContainerColor = MaterialTheme.extraColors.card,
    focusedTextColor = MaterialTheme.extraColors.ink,
    unfocusedTextColor = MaterialTheme.extraColors.ink
)
