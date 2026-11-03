package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.MatchRecord
import com.example.data.model.SwipeRecord
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class DatingRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java)
                } else null
            }
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val payload = hashMapOf<String, Any>(
            "id" to uid,
            "displayName" to profile.displayName.ifBlank { "Spark Member" },
            "age" to if (profile.age in 18..100) profile.age else 24,
            "bio" to profile.bio,
            "occupation" to profile.occupation,
            "location" to profile.location,
            "interests" to profile.interests,
            "photoUrl" to profile.photoUrl,
            "isPhoneVerified" to profile.isPhoneVerified,
            "verifiedPhoneNumber" to profile.verifiedPhoneNumber,
            "gender" to profile.gender,
            "interestedIn" to profile.interestedIn,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val doc = userRef.get().await()
        if (!doc.exists()) {
            payload["createdAt"] = FieldValue.serverTimestamp()
        }
        userRef.set(payload, SetOptions.merge()).await()
    }

    suspend fun verifyUserPhoneNumber(phoneNumber: String) {
        val uid = requireUserId()
        db.collection("users").document(uid).update(
            mapOf(
                "isPhoneVerified" to true,
                "verifiedPhoneNumber" to phoneNumber,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    fun observeDiscoveryProfiles(currentUserId: String): Flow<List<UserProfile>> = callbackFlow {
        val swipesRef = db.collection("users").document(currentUserId).collection("swipes")
        val usersRef = db.collection("users")

        val listener = usersRef.addSnapshotListener { usersSnapshot, usersError ->
            if (usersError != null) {
                Log.e("DatingRepository", "Error loading discovery profiles", usersError)
                trySend(emptyList())
                return@addSnapshotListener
            }

            swipesRef.get().addOnSuccessListener { swipesSnapshot ->
                val swipedUserIds = swipesSnapshot.documents.map { it.id }.toSet()
                val profiles = usersSnapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(UserProfile::class.java)
                }?.filter { profile ->
                    profile.id != currentUserId && !swipedUserIds.contains(profile.id)
                } ?: emptyList()

                trySend(profiles)
            }.addOnFailureListener {
                val profiles = usersSnapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(UserProfile::class.java)
                }?.filter { it.id != currentUserId } ?: emptyList()
                trySend(profiles)
            }
        }

        awaitClose { listener.remove() }
    }

    suspend fun recordSwipe(targetUserId: String, type: String): Boolean {
        val currentUserId = requireUserId()
        val userSwipeRef = db.collection("users").document(currentUserId)
            .collection("swipes").document(targetUserId)

        userSwipeRef.set(
            mapOf(
                "targetUserId" to targetUserId,
                "type" to type,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()

        if (type == "like") {
            // Check if reciprocal match exists
            val targetSwipe = db.collection("users").document(targetUserId)
                .collection("swipes").document(currentUserId).get().await()

            val isTargetLike = targetSwipe.exists() && targetSwipe.getString("type") == "like"
            // For demo engagement: if target is a demo profile or reciprocal like, trigger match
            val isDemoTarget = targetUserId.startsWith("demo_")
            val isMatch = isTargetLike || isDemoTarget

            if (isMatch) {
                createMatchAndConversation(currentUserId, targetUserId)
                return true
            }
        }
        return false
    }

    private suspend fun createMatchAndConversation(userA: String, userB: String) {
        val sortedUsers = listOf(userA, userB).sorted()
        val matchId = "${sortedUsers[0]}_${sortedUsers[1]}"
        val matchRef = db.collection("matches").document(matchId)

        matchRef.set(
            mapOf(
                "id" to matchId,
                "users" to sortedUsers,
                "matchedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()

        val convRef = db.collection("conversations").document(matchId)
        val convDoc = convRef.get().await()
        if (!convDoc.exists()) {
            convRef.set(
                mapOf(
                    "id" to matchId,
                    "participants" to sortedUsers,
                    "lastMessage" to "You matched! Say hello 👋",
                    "lastMessageSenderId" to "",
                    "lastMessageTimestamp" to FieldValue.serverTimestamp(),
                    "unreadCount" to mapOf(userA to 0L, userB to 0L),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    fun observeConversations(currentUserId: String): Flow<List<Conversation>> {
        return db.collection("conversations")
            .whereArrayContains("participants", currentUserId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Conversation::class.java)
                    .sortedByDescending { it.lastMessageTimestamp?.seconds ?: 0L }
            }
    }

    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> {
        return db.collection("conversations").document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ChatMessage::class.java)
            }
    }

    suspend fun sendMessage(conversationId: String, receiverId: String, text: String) {
        val currentUserId = requireUserId()
        val messagesRef = db.collection("conversations").document(conversationId).collection("messages")
        val newMessageRef = messagesRef.document()

        val messageData = mapOf(
            "id" to newMessageRef.id,
            "conversationId" to conversationId,
            "senderId" to currentUserId,
            "receiverId" to receiverId,
            "text" to text.trim(),
            "timestamp" to FieldValue.serverTimestamp(),
            "isRead" to false,
            "readAt" to null
        )

        newMessageRef.set(messageData).await()

        val convRef = db.collection("conversations").document(conversationId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(convRef)
            val unreadMap = (snapshot.get("unreadCount") as? Map<*, *>)?.toMutableMap() ?: mutableMapOf<Any?, Any?>()
            val currentReceiverCount = (unreadMap[receiverId] as? Number)?.toLong() ?: 0L
            unreadMap[receiverId] = currentReceiverCount + 1L

            transaction.update(
                convRef,
                mapOf(
                    "lastMessage" to text.trim(),
                    "lastMessageSenderId" to currentUserId,
                    "lastMessageTimestamp" to FieldValue.serverTimestamp(),
                    "unreadCount" to unreadMap
                )
            )
        }.await()
    }

    suspend fun markMessagesAsRead(conversationId: String) {
        val currentUserId = requireUserId()
        val convRef = db.collection("conversations").document(conversationId)

        // Reset unread count for current user in conversation
        try {
            db.runTransaction { transaction ->
                val snapshot = transaction.get(convRef)
                if (snapshot.exists()) {
                    val unreadMap = (snapshot.get("unreadCount") as? Map<*, *>)?.toMutableMap() ?: mutableMapOf<Any?, Any?>()
                    unreadMap[currentUserId] = 0L
                    transaction.update(convRef, "unreadCount", unreadMap)
                }
            }.await()
        } catch (e: Exception) {
            Log.w("DatingRepository", "Failed to clear unreadCount", e)
        }

        // Mark individual unread incoming messages as read
        val unreadMessagesQuery = convRef.collection("messages")
            .whereEqualTo("receiverId", currentUserId)
            .whereEqualTo("isRead", false)
            .get().await()

        for (doc in unreadMessagesQuery.documents) {
            try {
                doc.reference.update(
                    mapOf(
                        "isRead" to true,
                        "readAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (e: Exception) {
                Log.w("DatingRepository", "Failed to mark message read: ${doc.id}", e)
            }
        }
    }

    suspend fun seedDemoProfilesIfEmpty(currentUserId: String) {
        val count = db.collection("users").limit(4).get().await().size()
        if (count <= 1) {
            val demoProfiles = listOf(
                UserProfile(
                    id = "demo_ananya",
                    displayName = "Ananya Sharma",
                    age = 23,
                    bio = "Architect by day, salsa dancer by night 💃. Looking for someone who enjoys Sunday brunches and deep music conversations.",
                    occupation = "Interior Architect",
                    location = "Bandra, Mumbai",
                    interests = listOf("Architecture", "Coffee", "Salsa", "Indie Music", "Travel"),
                    photoUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=800&auto=format&fit=crop&q=80",
                    isPhoneVerified = true,
                    verifiedPhoneNumber = "+91 98200 •••••",
                    gender = "Woman",
                    interestedIn = "Everyone"
                ),
                UserProfile(
                    id = "demo_rohan",
                    displayName = "Rohan Mehta",
                    age = 26,
                    bio = "Tech entrepreneur & amateur chef 🍳. If you like mountain treks, filter coffee, and indie films, we will vibe!",
                    occupation = "Founder @ GreenTech",
                    location = "Koramangala, Bengaluru",
                    interests = listOf("Startups", "Trekking", "Cooking", "Photography", "Books"),
                    photoUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=800&auto=format&fit=crop&q=80",
                    isPhoneVerified = true,
                    verifiedPhoneNumber = "+91 98111 •••••",
                    gender = "Man",
                    interestedIn = "Everyone"
                ),
                UserProfile(
                    id = "demo_priya",
                    displayName = "Priya Nair",
                    age = 25,
                    bio = "Wildlife photographer & yoga lover 🌿. Currently on a mission to find the best dark chocolate gelato in town.",
                    occupation = "Visual Storyteller",
                    location = "Colaba, Mumbai",
                    interests = listOf("Yoga", "Wildlife", "Art Galleries", "Gelato", "Dogs"),
                    photoUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=800&auto=format&fit=crop&q=80",
                    isPhoneVerified = true,
                    verifiedPhoneNumber = "+91 98333 •••••",
                    gender = "Woman",
                    interestedIn = "Everyone"
                ),
                UserProfile(
                    id = "demo_kabir",
                    displayName = "Kabir Verma",
                    age = 27,
                    bio = "Guitarist in an acoustic band 🎸 and product designer. Tell me your favorite 90s rock song and let's grab iced matcha.",
                    occupation = "Product Designer",
                    location = "Indiranagar, Bengaluru",
                    interests = listOf("Guitar", "Rock Music", "UI/UX", "Matcha", "Board Games"),
                    photoUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=800&auto=format&fit=crop&q=80",
                    isPhoneVerified = true,
                    verifiedPhoneNumber = "+91 98444 •••••",
                    gender = "Man",
                    interestedIn = "Everyone"
                )
            )

            for (profile in demoProfiles) {
                db.collection("users").document(profile.id).set(
                    mapOf(
                        "id" to profile.id,
                        "displayName" to profile.displayName,
                        "age" to profile.age,
                        "bio" to profile.bio,
                        "occupation" to profile.occupation,
                        "location" to profile.location,
                        "interests" to profile.interests,
                        "photoUrl" to profile.photoUrl,
                        "isPhoneVerified" to profile.isPhoneVerified,
                        "verifiedPhoneNumber" to profile.verifiedPhoneNumber,
                        "gender" to profile.gender,
                        "interestedIn" to profile.interestedIn,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()
            }
        }
    }
}
