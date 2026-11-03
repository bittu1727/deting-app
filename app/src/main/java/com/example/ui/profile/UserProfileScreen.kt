package com.example.ui.profile

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import coil.compose.AsyncImage
import com.example.data.model.UserProfile
import com.example.ui.auth.signOut
import com.example.ui.theme.SparkPassRed
import com.example.ui.theme.SparkPrimary
import com.example.ui.theme.SparkSecondary
import com.example.ui.theme.SparkTertiary
import com.example.ui.theme.SparkVerifiedBlue

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserProfileScreen(
    userProfile: UserProfile?,
    onSaveProfile: (UserProfile) -> Unit,
    onOpenPhoneVerification: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var isEditing by remember { mutableStateOf(false) }
    var displayName by remember(userProfile) { mutableStateOf(userProfile?.displayName ?: "") }
    var ageText by remember(userProfile) { mutableStateOf(userProfile?.age?.toString() ?: "24") }
    var bio by remember(userProfile) { mutableStateOf(userProfile?.bio ?: "") }
    var occupation by remember(userProfile) { mutableStateOf(userProfile?.occupation ?: "") }
    var newInterest by remember { mutableStateOf("") }
    var currentInterests by remember(userProfile) {
        mutableStateOf(userProfile?.interests ?: listOf("Music", "Travel", "Coffee"))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar and Name Header
        item {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(SparkPrimary, SparkSecondary))
                    )
                    .padding(4.dp)
            ) {
                if (userProfile?.photoUrl?.isNotBlank() == true) {
                    AsyncImage(
                        model = userProfile.photoUrl,
                        contentDescription = "Your Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (userProfile?.displayName?.take(1) ?: "S"),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = SparkPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${userProfile?.displayName ?: "Spark Explorer"}, ${userProfile?.age ?: 24}",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (userProfile?.isPhoneVerified == true) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verified Profile",
                        tint = SparkVerifiedBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Text(
                text = userProfile?.location ?: "Mumbai, India",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Phone Verification Status Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("verification_status_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (userProfile?.isPhoneVerified == true)
                        SparkVerifiedBlue.copy(alpha = 0.12f)
                    else
                        SparkTertiary.copy(alpha = 0.14f)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (userProfile?.isPhoneVerified == true) SparkVerifiedBlue else SparkTertiary
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (userProfile?.isPhoneVerified == true) Icons.Default.Verified else Icons.Default.Security,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = if (userProfile?.isPhoneVerified == true)
                                    "Profile Verified ✓"
                                else
                                    "Profile Not Verified",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (userProfile?.isPhoneVerified == true) SparkVerifiedBlue else Color(0xFFC27D00)
                            )
                            Text(
                                text = if (userProfile?.isPhoneVerified == true)
                                    "Phone: ${userProfile.verifiedPhoneNumber}"
                                else
                                    "Verify via SMS OTP to earn a Blue Trust Badge",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (userProfile?.isPhoneVerified != true) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onOpenPhoneVerification,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("verify_phone_now_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SparkTertiary)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "Phone")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Verify Phone with OTP",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Edit Profile Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Profile Details",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        androidx.compose.material3.TextButton(
                            onClick = { isEditing = !isEditing },
                            modifier = Modifier.testTag("toggle_edit_profile_button")
                        ) {
                            Text(
                                text = if (isEditing) "Cancel" else "Edit",
                                color = SparkPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isEditing) {
                        // Read-only view
                        ProfileInfoItem(label = "Bio", value = userProfile?.bio?.ifBlank { "No bio added yet" } ?: "")
                        Spacer(modifier = Modifier.height(12.dp))
                        ProfileInfoItem(label = "Occupation", value = userProfile?.occupation?.ifBlank { "Not specified" } ?: "")
                        Spacer(modifier = Modifier.height(12.dp))
                        ProfileInfoItem(label = "Gender Preference", value = userProfile?.interestedIn ?: "Everyone")
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Interests & Passions",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            currentInterests.forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Editable Form
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("Display Name") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_name_field"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = ageText,
                            onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) ageText = it },
                            label = { Text("Age") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_age_field"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            label = { Text("Bio") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_bio_field"),
                            maxLines = 4,
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = occupation,
                            onValueChange = { occupation = it },
                            label = { Text("Occupation") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_occupation_field"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Add new interest
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newInterest,
                                onValueChange = { newInterest = it },
                                label = { Text("Add Interest tag") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (newInterest.isNotBlank() && !currentInterests.contains(newInterest.trim())) {
                                        currentInterests = currentInterests + newInterest.trim()
                                        newInterest = ""
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add tag", tint = SparkPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            currentInterests.forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = SparkPrimary.copy(alpha = 0.12f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = tag, style = MaterialTheme.typography.bodySmall, color = SparkPrimary)
                                        IconButton(
                                            onClick = { currentInterests = currentInterests - tag },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                val updated = (userProfile ?: UserProfile()).copy(
                                    displayName = displayName.trim(),
                                    age = ageText.toIntOrNull() ?: 24,
                                    bio = bio.trim(),
                                    occupation = occupation.trim(),
                                    interests = currentInterests
                                )
                                onSaveProfile(updated)
                                isEditing = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_profile_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SparkPrimary)
                        ) {
                            Text("Save Changes", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Sign Out Button
        item {
            OutlinedButton(
                onClick = {
                    signOut(
                        context = context,
                        credentialManager = credentialManager,
                        onSignOutComplete = onSignOut,
                        scope = coroutineScope
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sign_out_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SparkPassRed)
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign Out", tint = SparkPassRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out", fontWeight = FontWeight.Bold, color = SparkPassRed)
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun ProfileInfoItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}
