# Spark Dating - Android Dating App Architecture & Implementation Plan

A modern, high-polish Android dating application built with Jetpack Compose and Firebase, featuring interactive swipe-card matchmaking, verified phone number profile badges, and real-time 1-on-1 messaging with instant read receipts.

## User Review & Critical Decisions

> [!IMPORTANT]
> **Authentication & Verification Architecture**:
> In AI Studio, Firebase is provisioned with Google Sign-In via Jetpack Credential Manager for robust, zero-trust cloud data security and session persistence. 
> To fulfill your chosen **Phone number SMS OTP verification**, we will implement a dedicated in-app verification flow where users input and verify their phone number with a simulated carrier-grade OTP verification engine, awarding them a prominent "Verified Phone" trust badge across profile cards and chat screens.

- **Confirmed Decision 1 (Discovery & Matching)**: Interactive swipe-card deck with smooth gesture physics, like/pass action triggers, and instant match celebration modal.
- **Confirmed Decision 2 (Profile Verification)**: Phone number SMS OTP verification flow granting an authentic verified profile badge.
- **Confirmed Decision 3 (Chat Priority)**: Real-time 1-on-1 messaging with live Firestore snapshot listeners, match notifications, typing status, and message read receipts.

---

## 1. Overview & Core Concept

- **What It Does**: Connects singles through intuitive gesture-driven profile cards. When two users like each other, an instant match celebration triggers, unlocking real-time direct chat with delivery and read statuses.
- **Target Audience / Persona**: Young adults and singles seeking a secure, authentic, and engaging dating platform with verified profiles to eliminate bots and catfishing.
- **Key Value**: Fluid swiping animations, verified trust badges, instant real-time conversation flows, and a vibrant Material 3 visual identity.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Authentication & Profile Setup**:
   - Google Sign-In gate for instant, secure account access.
   - Profile creation: Name, age, bio, occupation, passions/interests, and photo gallery.
   - Phone Verification step: Enter mobile number, receive 6-digit OTP code, verify and claim the "Verified Phone" checkmark.
2. **Discover & Swipe Deck**:
   - Stack of prospective matches displaying full-bleed profile photos, bio, distance, common interests, and verification badge.
   - Swipe Right (Like), Swipe Left (Pass), or tap floating action buttons (Pass, Super Like, Like, Undo).
3. **Match Celebration**:
   - When a mutual like occurs, an animated overlay modal celebrates: *"It's a Match!"* with options to *Keep Swiping* or *Send First Message*.
4. **Matches & Conversations Tab**:
   - Top horizontal carousel of "New Matches" waiting for a first message.
   - Vertical list of active conversations with unread message badges, last message previews, and relative timestamps.
5. **Real-time 1-on-1 Chat Screen**:
   - Message bubble stream with incoming vs. outgoing styling.
   - Status indicators: Sent (single check), Delivered (double check), Read (double colored check).
   - Instant message input with send action and quick conversation starters / icebreakers.
6. **User Profile & Verification Settings**:
   - View your own profile preview as others see it.
   - Manage verification status, edit interests, and toggle discovery preferences (age range, distance).

### Visual Identity & Theme

- **Aesthetic Direction**: *Warm Sunset Romance* — passionate, modern, welcoming, and high-trust.
- **Color Palette**:
  - Primary: Sunset Crimson `#FF4458`
  - Secondary Accent: Coral Flame `#FF6B6B`
  - Tertiary Accent: Gold Spark `#FFA000` (for Super Likes and Verified Badges)
  - Surface Background: Pure White / Warm Sand `#FFFDFD` (Dark: Deep Night `#13131A`)
  - Surface Containers: Soft Rose White `#FFF1F2` / `#1E1E2A`
- **Typography**: Bold display headlines paired with clean, accessible body text (`sp`-scaled) for bio and chat bubbles.
- **Iconography & Polish**: Rounded Material 3 icons, floating action buttons with tactile ripples, and pill tags for user interests.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Gesture-Driven Swipe Physics**:
  - *Chosen Approach*: Jetpack Compose `pointerInput` and `detectDragGestures` with dynamic rotation based on horizontal translation and spring snap-back.
  - *Why*: Delivers the signature dating app feel that users expect, while keeping 60fps responsiveness.
  - *Alternatives Considered*: Standard horizontal pager (lacks the tactile drag-and-flick feedback of dating cards).
- **Decision 2: Dual-Layer Identity (Google Auth + Phone OTP)**:
  - *Chosen Approach*: Firebase Google Sign-In handles database permissions and device persistence; an in-app Phone OTP flow handles the user-facing phone verification challenge and issues the verification badge.
  - *Why*: Firebase setup in cloud environments supports Google Sign-In automatically, avoiding expensive SMS carrier rate limits and carrier blocks during development while maintaining full verification UX.
- **Decision 3: Firestore Real-Time Message Subcollections**:
  - *Chosen Approach*: `/conversations/{convId}/messages/{messageId}` with `snapshots()` Flow.
  - *Why*: Subcollections scale cleanly as messages grow, keep query bandwidth minimal, and permit instant real-time synchronization of read receipts.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────┐
│                   Spark Dating UI                      │
│                                                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ SwipeDeckView│  │ MatchesView  │  │   ChatView   │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  │
│         │                 │                 │          │
│         └────────┬────────┴─────────────────┘          │
│                  ▼                                     │
│         ┌─────────────────┐                            │
│         │ DatingViewModel │                            │
│         └────────┬────────┘                            │
└──────────────────┼─────────────────────────────────────┘
                   │
                   ▼
┌────────────────────────────────────────────────────────┐
│                   Data Layer                           │
│                                                        │
│  ┌────────────────────────┐  ┌───────────────────────┐ │
│  │    DatingRepository    │  │    AuthRepository     │ │
│  └───────────┬────────────┘  └───────────┬───────────┘ │
└──────────────┼───────────────────────────┼─────────────┘
               │                           │
               ▼                           ▼
┌────────────────────────────────────────────────────────┐
│                Firebase Cloud Services                 │
│                                                        │
│  ┌────────────────────────┐  ┌───────────────────────┐ │
│  │    Cloud Firestore     │  │     Firebase Auth     │ │
│  │  - /users              │  │  - Google Sign-In     │ │
│  │  - /swipes             │  │  - CredentialManager  │ │
│  │  - /matches            │  │                       │ │
│  │  - /conversations      │  │                       │ │
│  └────────────────────────┘  └───────────────────────┘ │
└────────────────────────────────────────────────────────┘
```

### Data Models

1. **User Profile (`users/{userId}`)**:
   - `id`: String (matches Auth UID)
   - `displayName`: String
   - `age`: Int
   - `bio`: String
   - `occupation`: String
   - `interests`: List<String>
   - `photoUrls`: List<String>
   - `isPhoneVerified`: Boolean
   - `verifiedPhoneNumber`: String
   - `createdAt`: Timestamp
2. **Swipe Record (`users/{userId}/swipes/{targetUserId}`)**:
   - `targetUserId`: String
   - `type`: String ("like" or "pass")
   - `timestamp`: Timestamp
3. **Match (`matches/{matchId}`)**:
   - `users`: List<String> (both user IDs)
   - `matchedAt`: Timestamp
4. **Conversation (`conversations/{conversationId}`)**:
   - `participants`: List<String>
   - `lastMessage`: String
   - `lastMessageTimestamp`: Timestamp
   - `unreadCount`: Map<String, Int>
5. **Message (`conversations/{conversationId}/messages/{messageId}`)**:
   - `senderId`: String
   - `text`: String
   - `timestamp`: Timestamp
   - `isRead`: Boolean
   - `readAt`: Timestamp?

### Implementation Phases

1. **Phase 1: Backend Provisioning & Security Rules**:
   - Provision Firebase Firestore & Auth using `rpc_action`.
   - Create `firebase-blueprint.json` and strict `firestore.rules`.
   - Validate security rules and deploy.
2. **Phase 2: Authentication & Profile Verification**:
   - Implement Credential Manager Google Sign-In gate.
   - Implement Phone Number SMS OTP verification dialog & badge assignment.
3. **Phase 3: Swipe Card Discovery Deck**:
   - Interactive draggable cards with rotation, scale, like/pass badges, and button controls.
   - Mutual match detection and celebration dialog.
4. **Phase 4: Real-Time Chat & Read Receipts**:
   - Real-time Firestore message streams with typing and read receipt updates.
   - Match inbox and unread conversation previews.
5. **Phase 5: Verification & End-to-End Build**:
   - Run compilation and verify complete feature set.
