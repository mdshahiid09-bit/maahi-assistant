package com.maahiassistant.android.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maahiassistant.android.ui.theme.ClayBlueAccent
import com.maahiassistant.android.ui.theme.ClaySurface
import com.maahiassistant.android.ui.theme.ClayTextPrimary
import com.maahiassistant.android.ui.theme.ClayTextSecondary
import com.maahiassistant.android.ui.theme.OrbCrimson

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "Confirm",
    cancelText: String = "Cancel",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ClayTextPrimary
            )
        },
        text = {
            Text(
                text = message,
                fontSize = 14.sp,
                color = ClayTextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) OrbCrimson else ClayBlueAccent,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = confirmText, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = cancelText, color = ClayTextPrimary)
            }
        },
        containerColor = ClaySurface,
        shape = RoundedCornerShape(20.dp)
    )
}
