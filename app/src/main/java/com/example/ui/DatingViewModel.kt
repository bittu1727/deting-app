package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.UserProfile
import com.example.data.repository.DatingRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class DatingTab {
    DISCOVER,
    MATCHES,
    PROFILE,
    CHAT_DETAIL
}

class DatingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DatingRepository(application)
    private val auth = Firebase.auth

    private val _currentTab = MutableStateFlow(DatingTab.DISCOVER)
    val currentTab: StateFlow<DatingTab> = _currentTab.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _discoveryProfiles = MutableStateFlow<List<UserProfile>>(emptyList())
    val discoveryProfiles: StateFlow<List<UserProfile>> = _discoveryProfiles.asStateFlow()

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    private val _activeChatPartner = MutableStateFlow<UserProfile?>(null)
    val activeChatPartner: StateFlow<UserProfile?> = _activeChatPartner.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    // Match celebration dialog
    private val _matchedProfile = MutableStateFlow<UserProfile?>(null)
    val matchedProfile: StateFlow<UserProfile?> = _matchedProfile.asStateFlow()

    // Phone verification dialog state
    private val _showPhoneVerification = MutableStateFlow(false)
    val showPhoneVerification: StateFlow<Boolean> = _showPhoneVerification.asStateFlow()

    private val _isOtpSent = MutableStateFlow(false)
    val isOtpSent: StateFlow<Boolean> = _isOtpSent.asStateFlow()

    private val _phoneInput = MutableStateFlow("")
    val phoneInput: StateFlow<String> = _phoneInput.asStateFlow()

    private val _otpInput = MutableStateFlow("")
    val otpInput: StateFlow<String> = _otpInput.asStateFlow()

    private val _otpError = MutableStateFlow<String?>(null)
    val otpError: StateFlow<String?> = _otpError.asStateFlow()

    private val _isVerifying = MutableStateFlow(false)
    val isVerifying: StateFlow<Boolean> = _isVerifying.asStateFlow()

    fun switchTab(tab: DatingTab) {
        _currentTab.value = tab
    }

    fun initSession() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            // Seed demo accounts if empty
            repository.seedDemoProfilesIfEmpty(uid)

            // Ensure profile exists for current user
            val currentUser = auth.currentUser
            val initialProfile = UserProfile(
                id = uid,
                displayName = currentUser?.displayName ?: "Spark Explorer",
                photoUrl = currentUser?.photoUrl?.toString() ?: "",
                age = 24,
                bio = "Exploring matches on Spark ✨",
                occupation = "Creative Professional",
                interests = listOf("Travel", "Music", "Coffee", "Tech")
            )
            repository.saveUserProfile(initialProfile)

            // Observe current user profile
            launch {
                repository.observeUserProfile(uid).collectLatest { profile ->
                    _userProfile.value = profile
                }
            }

            // Observe discovery profiles
            launch {
                repository.observeDiscoveryProfiles(uid).collectLatest { profiles ->
                    _discoveryProfiles.value = profiles
                }
            }

            // Observe conversations
            launch {
                repository.observeConversations(uid).collectLatest { convs ->
                    _conversations.value = convs
                }
            }
        }
    }

    fun swipeCurrentCard(profile: UserProfile, isLike: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val type = if (isLike) "like" else "pass"
            val isMatch = repository.recordSwipe(profile.id, type)
            if (isMatch) {
                _matchedProfile.value = profile
            }
        }
    }

    fun dismissMatchDialog() {
        _matchedProfile.value = null
    }

    fun openChatWithMatchedUser(profile: UserProfile) {
        val uid = auth.currentUser?.uid ?: return
        val sortedUsers = listOf(uid, profile.id).sorted()
        val convId = "${sortedUsers[0]}_${sortedUsers[1]}"
        _matchedProfile.value = null
        openConversation(convId, profile)
    }

    fun openConversation(conversationId: String, partner: UserProfile?) {
        _activeConversationId.value = conversationId
        _activeChatPartner.value = partner
        _currentTab.value = DatingTab.CHAT_DETAIL

        viewModelScope.launch {
            repository.markMessagesAsRead(conversationId)
            repository.observeMessages(conversationId).collectLatest { msgs ->
                _chatMessages.value = msgs
            }
        }
    }

    fun closeChat() {
        _activeConversationId.value = null
        _activeChatPartner.value = null
        _chatMessages.value = emptyList()
        _currentTab.value = DatingTab.MATCHES
    }

    fun sendMessage(text: String) {
        val convId = _activeConversationId.value ?: return
        val partnerId = _activeChatPartner.value?.id ?: return
        if (text.isBlank()) return

        viewModelScope.launch {
            repository.sendMessage(convId, partnerId, text)
        }
    }

    // Phone verification methods
    fun openPhoneVerification() {
        _showPhoneVerification.value = true
        _isOtpSent.value = false
        _phoneInput.value = ""
        _otpInput.value = ""
        _otpError.value = null
    }

    fun closePhoneVerification() {
        _showPhoneVerification.value = false
        _isOtpSent.value = false
        _phoneInput.value = ""
        _otpInput.value = ""
        _otpError.value = null
    }

    fun setPhoneInput(phone: String) {
        _phoneInput.value = phone
        _otpError.value = null
    }

    fun setOtpInput(otp: String) {
        if (otp.length <= 6) {
            _otpInput.value = otp
            _otpError.value = null
        }
    }

    fun sendOtp() {
        val phone = _phoneInput.value.trim()
        if (phone.length < 10) {
            _otpError.value = "Please enter a valid 10-digit phone number"
            return
        }
        _isOtpSent.value = true
        _otpError.value = null
        // Auto-fill or simulate realistic carrier OTP code: "749201"
        _otpInput.value = "749201"
    }

    fun verifyOtp() {
        val otp = _otpInput.value.trim()
        if (otp.length != 6) {
            _otpError.value = "Please enter complete 6-digit OTP"
            return
        }
        _isVerifying.value = true
        viewModelScope.launch {
            try {
                val formattedPhone = if (_phoneInput.value.startsWith("+")) _phoneInput.value else "+91 ${_phoneInput.value}"
                repository.verifyUserPhoneNumber(formattedPhone)
                _isVerifying.value = false
                _showPhoneVerification.value = false
            } catch (e: Exception) {
                _isVerifying.value = false
                _otpError.value = "Verification failed: ${e.message}"
            }
        }
    }

    fun updateUserProfile(updated: UserProfile) {
        viewModelScope.launch {
            repository.saveUserProfile(updated)
        }
    }
}
