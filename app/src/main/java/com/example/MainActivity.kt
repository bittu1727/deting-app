package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.DatingTab
import com.example.ui.DatingViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.chat.ChatDetailScreen
import com.example.ui.chat.ConversationsScreen
import com.example.ui.components.PhoneVerificationDialog
import com.example.ui.discovery.SwipeDeckScreen
import com.example.ui.profile.UserProfileScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SparkPrimary
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppGate()
            }
        }
    }
}

@Composable
fun AppGate() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        MainDatingApp(
            currentUserId = currentUser!!.uid,
            onSignOut = {
                currentUser = null
            }
        )
    }
}

@Composable
fun MainDatingApp(
    currentUserId: String,
    onSignOut: () -> Unit,
    viewModel: DatingViewModel = viewModel()
) {
    LaunchedEffect(currentUserId) {
        viewModel.initSession()
    }

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val discoveryProfiles by viewModel.discoveryProfiles.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val activePartner by viewModel.activeChatPartner.collectAsStateWithLifecycle()
    val matchedProfile by viewModel.matchedProfile.collectAsStateWithLifecycle()

    // Phone verification dialog state
    val showPhoneDialog by viewModel.showPhoneVerification.collectAsStateWithLifecycle()
    val isOtpSent by viewModel.isOtpSent.collectAsStateWithLifecycle()
    val phoneInput by viewModel.phoneInput.collectAsStateWithLifecycle()
    val otpInput by viewModel.otpInput.collectAsStateWithLifecycle()
    val otpError by viewModel.otpError.collectAsStateWithLifecycle()
    val isVerifying by viewModel.isVerifying.collectAsStateWithLifecycle()

    // Back handling
    if (currentTab == DatingTab.CHAT_DETAIL) {
        BackHandler {
            viewModel.closeChat()
        }
    } else if (currentTab != DatingTab.DISCOVER) {
        BackHandler {
            viewModel.switchTab(DatingTab.DISCOVER)
        }
    }

    // Calculate total unread count
    val totalUnread = conversations.sumOf { it.unreadCount[currentUserId] ?: 0L }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentTab != DatingTab.CHAT_DETAIL) {
                NavigationBar(
                    modifier = Modifier.testTag("dating_bottom_navigation")
                ) {
                    NavigationBarItem(
                        selected = currentTab == DatingTab.DISCOVER,
                        onClick = { viewModel.switchTab(DatingTab.DISCOVER) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == DatingTab.DISCOVER) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Discover",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text("Discover") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SparkPrimary,
                            selectedTextColor = SparkPrimary
                        ),
                        modifier = Modifier.testTag("nav_item_discover")
                    )

                    NavigationBarItem(
                        selected = currentTab == DatingTab.MATCHES,
                        onClick = { viewModel.switchTab(DatingTab.MATCHES) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (totalUnread > 0) {
                                        Badge { Text("$totalUnread") }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (currentTab == DatingTab.MATCHES) Icons.Default.ChatBubble else Icons.Outlined.ChatBubbleOutline,
                                    contentDescription = "Matches & Chats",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        label = { Text("Matches") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SparkPrimary,
                            selectedTextColor = SparkPrimary
                        ),
                        modifier = Modifier.testTag("nav_item_matches")
                    )

                    NavigationBarItem(
                        selected = currentTab == DatingTab.PROFILE,
                        onClick = { viewModel.switchTab(DatingTab.PROFILE) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == DatingTab.PROFILE) Icons.Default.Person else Icons.Outlined.PersonOutline,
                                contentDescription = "Profile",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text("Profile") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SparkPrimary,
                            selectedTextColor = SparkPrimary
                        ),
                        modifier = Modifier.testTag("nav_item_profile")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                DatingTab.DISCOVER -> {
                    SwipeDeckScreen(
                        profiles = discoveryProfiles,
                        currentUserProfile = userProfile,
                        matchedProfile = matchedProfile,
                        onSwipe = { profile, isLike ->
                            viewModel.swipeCurrentCard(profile, isLike)
                        },
                        onDismissMatch = {
                            viewModel.dismissMatchDialog()
                        },
                        onOpenChat = { profile ->
                            viewModel.openChatWithMatchedUser(profile)
                        },
                        onReload = {
                            viewModel.initSession()
                        }
                    )
                }

                DatingTab.MATCHES -> {
                    ConversationsScreen(
                        currentUserId = currentUserId,
                        conversations = conversations,
                        allProfiles = discoveryProfiles,
                        onSelectConversation = { convId, partner ->
                            viewModel.openConversation(convId, partner)
                        },
                        onStartDiscover = {
                            viewModel.switchTab(DatingTab.DISCOVER)
                        }
                    )
                }

                DatingTab.CHAT_DETAIL -> {
                    ChatDetailScreen(
                        currentUserId = currentUserId,
                        partner = activePartner,
                        messages = chatMessages,
                        onSendMessage = { text ->
                            viewModel.sendMessage(text)
                        },
                        onBack = {
                            viewModel.closeChat()
                        }
                    )
                }

                DatingTab.PROFILE -> {
                    UserProfileScreen(
                        userProfile = userProfile,
                        onSaveProfile = { updated ->
                            viewModel.updateUserProfile(updated)
                        },
                        onOpenPhoneVerification = {
                            viewModel.openPhoneVerification()
                        },
                        onSignOut = onSignOut
                    )
                }
            }
        }

        // Phone Verification Modal
        PhoneVerificationDialog(
            show = showPhoneDialog,
            isOtpSent = isOtpSent,
            phoneInput = phoneInput,
            otpInput = otpInput,
            otpError = otpError,
            isVerifying = isVerifying,
            onPhoneChanged = { viewModel.setPhoneInput(it) },
            onOtpChanged = { viewModel.setOtpInput(it) },
            onSendOtp = { viewModel.sendOtp() },
            onVerifyOtp = { viewModel.verifyOtp() },
            onDismiss = { viewModel.closePhoneVerification() }
        )
    }
}
