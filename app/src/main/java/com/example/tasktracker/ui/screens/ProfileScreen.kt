package com.example.tasktracker.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.tasktracker.ui.components.SegmentedControl
import com.example.tasktracker.ui.model.ThemeMode
import com.example.tasktracker.ui.theme.extraColors
import java.io.File
import java.io.FileOutputStream

@Composable
fun ProfileScreen(
    userUid: String?,
    userEmail: String?,
    profileName: String,
    profileContact: String,
    profileImageUri: String,
    themeMode: ThemeMode,
    isSyncing: Boolean,
    authError: String?,
    infoMessage: String?,
    onSignIn: (email: String, pass: String) -> Unit,
    onRegister: (name: String, contact: String, email: String, pass: String) -> Unit,
    onForgotPassword: () -> Unit,
    onUpdateProfileImage: (String) -> Unit,
    onUpdateProfileDetails: (name: String, contact: String) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onLogout: () -> Unit,
    onClearMessages: () -> Unit,
    syncStatus: String? = null,
    onSyncNow: () -> Unit = {},
    notificationsEnabled: Boolean = true,
    defaultReminderTime: String = "05:00",
    onSetNotificationsEnabled: (Boolean) -> Unit = {},
    onSetDefaultReminderTime: (hhmm: String, applyToAll: Boolean) -> Unit = { _, _ -> }
) {
    val colors = MaterialTheme.extraColors
    val isLoggedIn = !userUid.isNullOrEmpty()

    var authMode by remember { mutableStateOf("signin") } // "signin" or "register"
    var nameInput by remember { mutableStateOf("") }
    var contactInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Masked / Hidden User ID State
    var isUidVisible by remember { mutableStateOf(false) }

    // Edit Profile Details Dialog State
    var isEditingDetails by remember { mutableStateOf(false) }

    // Crop Image Dialog State
    var uncroppedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { uncroppedImageUri = it }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Profile",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.ink,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            if (!isLoggedIn) {
                // SIGN IN / REGISTER FORM WHEN SIGNED OUT
                Text(
                    text = "ACCOUNT",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.muted,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.card)
                        .border(1.dp, colors.line, RoundedCornerShape(10.dp))
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (authMode == "signin") colors.accent else Color.Transparent)
                            .clickable {
                                authMode = "signin"
                                onClearMessages()
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sign In",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (authMode == "signin") colors.paper else colors.ink
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (authMode == "register") colors.accent else Color.Transparent)
                            .clickable {
                                authMode = "register"
                                onClearMessages()
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Register",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (authMode == "register") colors.paper else colors.ink
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

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
                    Spacer(Modifier.height(10.dp))
                }

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
                    Spacer(Modifier.height(10.dp))
                }

                if (authMode == "signin") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Email (Required)") },
                            singleLine = true,
                            colors = textFieldColors(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Password (Required)") },
                            singleLine = true,
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle Password Visibility",
                                        tint = colors.muted
                                    )
                                }
                            },
                            colors = textFieldColors(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        TextButton(
                            onClick = onForgotPassword,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Forgot Password?", color = colors.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { onSignIn(emailInput.trim(), passwordInput) },
                            enabled = !isSyncing,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.paper),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Sign In", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Name (Optional)") },
                            singleLine = true,
                            colors = textFieldColors(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = contactInput,
                            onValueChange = { contactInput = it },
                            label = { Text("Contact Number (Optional)") },
                            singleLine = true,
                            colors = textFieldColors(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Email (Required)") },
                            singleLine = true,
                            colors = textFieldColors(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Password (Required)") },
                            singleLine = true,
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle Password Visibility",
                                        tint = colors.muted
                                    )
                                }
                            },
                            colors = textFieldColors(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                onRegister(nameInput.trim(), contactInput.trim(), emailInput.trim(), passwordInput)
                                authMode = "signin"
                            },
                            enabled = !isSyncing,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.paper),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Register", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (isSyncing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            } else {
                // SIGNED IN: DEDICATED PROFILE PAGE
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 90.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. PROFILE IMAGE WITH EDIT / UPLOAD OPTIONS
                    Box(
                        contentAlignment = Alignment.BottomEnd,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        if (profileImageUri.isNotBlank()) {
                            AsyncImage(
                                model = profileImageUri,
                                contentDescription = "Profile Picture",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape)
                                    .border(3.dp, colors.accent, CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape)
                                    .background(colors.accentSoft)
                                    .border(3.dp, colors.accent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                val initial = (profileName.ifBlank { userEmail ?: "A" }).trim().take(1).uppercase()
                                Text(
                                    text = initial,
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accent
                                )
                            }
                        }

                        // Camera/Edit Icon Overlay
                        SmallFloatingActionButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            containerColor = colors.accent,
                            contentColor = colors.paper,
                            shape = CircleShape,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Upload Picture", modifier = Modifier.size(18.dp))
                        }
                    }

                    TextButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Text(
                            text = if (profileImageUri.isBlank()) "Upload Profile Picture" else "Resize / Change Picture",
                            color = colors.accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // 2. USER DETAILS CARD WITH EDIT BUTTON
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.card)
                            .border(1.dp, colors.line, RoundedCornerShape(18.dp))
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "USER DETAILS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.muted
                            )

                            TextButton(onClick = { isEditingDetails = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Profile", modifier = Modifier.size(16.dp), tint = colors.accent)
                                Spacer(Modifier.width(4.dp))
                                Text("Edit Details", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                            }
                        }

                        DetailItem(label = "USERNAME / NAME", value = profileName.ifBlank { "You" })

                        HorizontalDivider(color = colors.line.copy(alpha = 0.5f))

                        DetailItem(label = "EMAIL", value = userEmail ?: "No email")

                        HorizontalDivider(color = colors.line.copy(alpha = 0.5f))

                        DetailItem(label = "CONTACT NUMBER", value = profileContact.ifBlank { "Not provided" })

                        HorizontalDivider(color = colors.line.copy(alpha = 0.5f))

                        // MASKED / HIDDEN USER ID WITH UNHIDE TOGGLE
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "USER ID / UID",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.muted
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { isUidVisible = !isUidVisible }
                                ) {
                                    Text(
                                        text = if (isUidVisible) "Hide ID" else "Unhide ID",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.accent
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (isUidVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle UID Visibility",
                                        tint = colors.accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            val maskedUid = remember(userUid) {
                                val id = userUid ?: "—"
                                if (id.length > 8) id.take(4) + "••••••••••••••••" else "••••••••••••••••"
                            }

                            Text(
                                text = if (isUidVisible) (userUid ?: "—") else maskedUid,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // 3. AUTOMATIC DEVICE THEME DETECTOR SELECTOR
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.card)
                            .border(1.dp, colors.line, RoundedCornerShape(18.dp))
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "APP THEME",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.muted
                        )

                        SegmentedControl(
                            options = listOf(
                                ThemeMode.SYSTEM.id to "Auto (Device)",
                                ThemeMode.LIGHT.id to "Light",
                                ThemeMode.DARK.id to "Dark"
                            ),
                            selectedId = themeMode.id,
                            onSelect = { id ->
                                ThemeMode.entries.firstOrNull { it.id == id }?.let { onSetThemeMode(it) }
                            }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // NOTIFICATION SETTINGS: on/off (default ON) + default time (default 5:00 AM)
                    NotificationSettingsCard(
                        enabled = notificationsEnabled,
                        defaultTime = defaultReminderTime,
                        onSetEnabled = onSetNotificationsEnabled,
                        onSetTime = onSetDefaultReminderTime
                    )

                    Spacer(Modifier.height(28.dp))

                    // Sync is automatic (every 30 seconds). Only a failure is shown, so it is never silent.
                    if (!syncStatus.isNullOrBlank() && syncStatus.startsWith("Sync failed")) {
                        Text(
                            text = syncStatus,
                            fontSize = 12.sp,
                            color = colors.high
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    // 4. LOGOUT BUTTON AT THE VERY BOTTOM OF THE PROFILE PAGE
                    OutlinedButton(
                        onClick = onLogout,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.high),
                        border = BorderStroke(1.dp, colors.high),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Logout", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // EDIT USER DETAILS DIALOG
    if (isEditingDetails) {
        var editName by remember { mutableStateOf(profileName) }
        var editContact by remember { mutableStateOf(profileContact) }

        AlertDialog(
            onDismissRequest = { isEditingDetails = false },
            containerColor = colors.paper,
            title = { Text("Edit Profile Details", fontWeight = FontWeight.Bold, color = colors.ink) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Name / Username") },
                        singleLine = true,
                        colors = textFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editContact,
                        onValueChange = { editContact = it },
                        label = { Text("Contact Number") },
                        singleLine = true,
                        colors = textFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateProfileDetails(editName.trim(), editContact.trim())
                        isEditingDetails = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.paper)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isEditingDetails = false }) {
                    Text("Cancel", color = colors.muted)
                }
            }
        )
    }

    // IMAGE CROPPING & RESIZING DIALOG
    uncroppedImageUri?.let { uri ->
        ProfileImageCropDialog(
            imageUri = uri,
            onCropSaved = { croppedUriStr ->
                onUpdateProfileImage(croppedUriStr)
                uncroppedImageUri = null
            },
            onDismiss = { uncroppedImageUri = null }
        )
    }
}

@Composable
fun ProfileImageCropDialog(
    imageUri: Uri,
    onCropSaved: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = MaterialTheme.extraColors

    val bitmap = remember(imageUri) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, imageUri)) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
            }
        } catch (_: Exception) {
            null
        }
    }

    if (bitmap == null) {
        SideEffect { onDismiss() }
        return
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.paper,
        title = { Text("Crop Profile Picture", fontWeight = FontWeight.Bold, color = colors.ink) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Pinch or drag the image/slider to resize and position",
                    fontSize = 12.sp,
                    color = colors.muted
                )

                // Interactive Crop Viewport Box
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .border(3.dp, colors.accent, CircleShape)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 4f)
                                offsetX += pan.x
                                offsetY += pan.y
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Crop Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                    )
                }

                // Zoom Slider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text("Zoom", fontSize = 12.sp, color = colors.muted)
                    Slider(
                        value = scale,
                        onValueChange = { scale = it },
                        valueRange = 0.5f..4f,
                        colors = SliderDefaults.colors(thumbColor = colors.accent, activeTrackColor = colors.accent),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val croppedUri = cropAndSaveProfileImage(context, bitmap, scale, offsetX, offsetY)
                    if (croppedUri != null) {
                        onCropSaved(croppedUri.toString())
                    } else {
                        onCropSaved(imageUri.toString())
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.paper)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.muted)
            }
        }
    )
}

private fun cropAndSaveProfileImage(
    context: Context,
    srcBitmap: Bitmap,
    scale: Float,
    offsetX: Float,
    offsetY: Float
): Uri? {
    return try {
        val size = 500
        val cropped = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(cropped)

        val srcWidth = srcBitmap.width.toFloat()
        val srcHeight = srcBitmap.height.toFloat()
        val baseScale = Math.max(size / srcWidth, size / srcHeight) * scale

        val matrix = Matrix().apply {
            postTranslate(-srcWidth / 2f, -srcHeight / 2f)
            postScale(baseScale, baseScale)
            postTranslate(size / 2f + (offsetX * (size / 220f)), size / 2f + (offsetY * (size / 220f)))
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
        }

        canvas.drawBitmap(srcBitmap, matrix, paint)

        val file = File(context.filesDir, "profile_cropped_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            cropped.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        Uri.fromFile(file)
    } catch (_: Exception) {
        null
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    val colors = MaterialTheme.extraColors
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = colors.muted
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.ink
        )
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationSettingsCard(
    enabled: Boolean,
    defaultTime: String,
    onSetEnabled: (Boolean) -> Unit,
    onSetTime: (String, Boolean) -> Unit
) {
    val colors = MaterialTheme.extraColors
    val context = LocalContext.current
    var showPicker by remember { mutableStateOf(false) }
    var pendingTime by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.card)
            .border(1.dp, colors.line, RoundedCornerShape(18.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("NOTIFICATIONS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.muted)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Task notifications", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.ink)
                Text(
                    text = if (enabled) "ON - every task sends its notification" else "OFF - no task notifications",
                    fontSize = 12.sp, color = colors.muted
                )
            }
            Switch(checked = enabled, onCheckedChange = onSetEnabled)
        }

        HorizontalDivider(color = colors.line)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { showPicker = true }
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Notification time", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (enabled) colors.ink else colors.muted)
                Text(
                    text = "Default 5:00 AM. All tasks notify at this time unless a task has its own time.",
                    fontSize = 12.sp, color = colors.muted
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = com.example.tasktracker.reminder.NotificationSettings.display(defaultTime),
                fontSize = 15.sp, fontWeight = FontWeight.Bold,
                color = if (enabled) colors.accent else colors.muted
            )
        }

        // Android 12+ may block exact alarms: without this the notification can arrive a few minutes late.
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            if (!am.canScheduleExactAlarms()) {
                Text(
                    text = "Allow exact alarms so notifications arrive exactly at the chosen time.",
                    fontSize = 12.sp, color = colors.high
                )
                TextButton(onClick = {
                    context.startActivity(
                        android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            .setData(Uri.parse("package:" + context.packageName))
                    )
                }) { Text("Allow exact time", fontWeight = FontWeight.Bold) }
            }
        }
    }

    if (showPicker) {
        val parts = defaultTime.split(":")
        val timeState = rememberTimePickerState(
            initialHour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 5,
            initialMinute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pendingTime = "%02d:%02d".format(timeState.hour, timeState.minute)
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = timeState) }
        )
    }

    pendingTime?.let { t ->
        AlertDialog(
            onDismissRequest = { pendingTime = null },
            title = { Text("Notification time: ${com.example.tasktracker.reminder.NotificationSettings.display(t)}") },
            text = { Text("Also change the time of tasks that have their own custom time?") },
            confirmButton = {
                TextButton(onClick = { onSetTime(t, true); pendingTime = null }) { Text("Yes, all tasks") }
            },
            dismissButton = {
                TextButton(onClick = { onSetTime(t, false); pendingTime = null }) { Text("No, keep their times") }
            }
        )
    }
}
