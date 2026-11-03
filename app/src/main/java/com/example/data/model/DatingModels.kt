package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val displayName: String = "",
    val age: Int = 24,
    val bio: String = "",
    val occupation: String = "",
    val location: String = "Mumbai, India",
    val interests: List<String> = emptyList(),
    val photoUrl: String = "",
    val isPhoneVerified: Boolean = false,
    val verifiedPhoneNumber: String = "",
    val gender: String = "Woman",
    val interestedIn: String = "Everyone",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SwipeRecord(
    @DocumentId val id: String = "",
    val targetUserId: String = "",
    val type: String = "like", // "like" or "pass"
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MatchRecord(
    @DocumentId val id: String = "",
    val users: List<String> = emptyList(),
    @ServerTimestamp val matchedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageSenderId: String = "",
    @ServerTimestamp val lastMessageTimestamp: Timestamp? = null,
    val unreadCount: Map<String, Long> = emptyMap(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null,
    val isRead: Boolean = false,
    @ServerTimestamp val readAt: Timestamp? = null
)
