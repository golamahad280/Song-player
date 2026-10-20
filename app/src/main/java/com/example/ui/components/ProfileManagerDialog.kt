package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.UserProfile
import com.example.ui.theme.SpotifyBlack
import com.example.ui.theme.SpotifyCardBackground
import com.example.ui.theme.SpotifyCardHover
import com.example.ui.theme.SpotifyDivider
import com.example.ui.theme.SpotifyElevated
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SpotifyTextMuted
import com.example.ui.theme.SpotifyTextPrimary
import com.example.ui.theme.SpotifyTextSecondary

@Composable
fun ProfileManagerDialog(
    currentProfile: UserProfile?,
    profiles: List<UserProfile>,
    onDismiss: () -> Unit,
    onSwitchProfile: (String) -> Unit,
    onCreateProfile: (String, Long) -> Unit,
    onDeleteProfile: (String) -> Unit
) {
    var isCreatingNew by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableLongStateOf(0xFF1ED760) }

    val presetColors = listOf(
        0xFF1ED760, // Spotify Green
        0xFF0D73EC, // Electric Blue
        0xFFE91429, // Vibrant Red
        0xFF9C27B0, // Royal Purple
        0xFFFF9800, // Sunset Orange
        0xFF009688  // Teal
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp)),
            color = SpotifyCardBackground,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_sp_logo),
                            contentDescription = "Song Player SP Logo",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isCreatingNew) "Create Profile" else "Song Player Profiles",
                            color = SpotifyTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SpotifyTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isCreatingNew) {
                    // Create Profile Form
                    Text(
                        text = "Enter a name and pick an accent color for this listener profile.",
                        color = SpotifyTextSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = newProfileName,
                        onValueChange = { newProfileName = it },
                        label = { Text("Profile Name", color = SpotifyTextSecondary) },
                        placeholder = { Text("e.g. Work, Workout, Chill", color = SpotifyTextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SpotifyElevated,
                            unfocusedContainerColor = SpotifyElevated,
                            focusedBorderColor = SpotifyGreen,
                            unfocusedBorderColor = SpotifyDivider,
                            focusedTextColor = SpotifyTextPrimary,
                            unfocusedTextColor = SpotifyTextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_profile_name_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Choose Avatar Color",
                        color = SpotifyTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(presetColors) { colorHex ->
                            val isSelected = selectedColor == colorHex
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorHex))
                                    .clickable { selectedColor = colorHex }
                                    .then(
                                        if (isSelected) Modifier.border(3.dp, Color.White, CircleShape)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { isCreatingNew = false },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Cancel", color = SpotifyTextSecondary)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = {
                                if (newProfileName.isNotBlank()) {
                                    onCreateProfile(newProfileName, selectedColor)
                                    isCreatingNew = false
                                    onDismiss()
                                }
                            },
                            enabled = newProfileName.isNotBlank(),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                            modifier = Modifier.testTag("confirm_create_profile_button")
                        ) {
                            Text("Create & Switch", color = SpotifyBlack, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Profile List
                    Text(
                        text = "Profiles let different listeners keep distinct custom playlists and preferences on this device.",
                        color = SpotifyTextSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        items(profiles) { profile ->
                            val isActive = profile.id == currentProfile?.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isActive) SpotifyElevated else SpotifyCardHover)
                                    .clickable {
                                        if (!isActive) {
                                            onSwitchProfile(profile.id)
                                            onDismiss()
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .testTag("profile_item_${profile.id}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar circle
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color(profile.avatarColor)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = profile.initials,
                                        color = SpotifyBlack,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = profile.name,
                                        color = SpotifyTextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isActive) {
                                        Text(
                                            text = "Active Profile",
                                            color = SpotifyGreen,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                if (isActive) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Active",
                                        tint = SpotifyGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else if (profiles.size > 1) {
                                    IconButton(
                                        onClick = { onDeleteProfile(profile.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Profile",
                                            tint = SpotifyTextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { isCreatingNew = true },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_new_profile_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = SpotifyBlack)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add New Profile", color = SpotifyBlack, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
