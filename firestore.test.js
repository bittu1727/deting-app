const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read users", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").get());
});

test("Authenticated user: can create their own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      id: ALICE_UID,
      displayName: "Alice",
      age: 24,
      bio: "Coffee lover and hiker",
      occupation: "Designer",
      interests: ["Coffee", "Art", "Hiking"],
      photoUrl: "",
      isPhoneVerified: true,
      verifiedPhoneNumber: "+91 9876543210",
      gender: "Female",
      interestedIn: "Male",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: cannot update another user's profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      id: BOB_UID,
      displayName: "Bob Hacked",
    })
  );
});

test("Authenticated user: can record a swipe in their own swipes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("swipes").doc(BOB_UID).set({
      targetUserId: BOB_UID,
      type: "like",
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: cannot write to another user's swipes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("swipes").doc(ALICE_UID).set({
      targetUserId: ALICE_UID,
      type: "like",
      createdAt: new Date(),
    })
  );
});

test("Chat: participants can send and read messages", async () => {
  const convId = "conv_alice_bob";
  // Seed conversation
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("conversations").doc(convId).set({
      id: convId,
      participants: [ALICE_UID, BOB_UID],
      lastMessage: "Hi",
      lastMessageSenderId: ALICE_UID,
      lastMessageTimestamp: new Date(),
      unreadCount: { [BOB_UID]: 1, [ALICE_UID]: 0 },
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("conversations").doc(convId).collection("messages").doc("msg1").set({
      id: "msg1",
      conversationId: convId,
      senderId: ALICE_UID,
      receiverId: BOB_UID,
      text: "Hey Bob! How are you?",
      timestamp: new Date(),
      isRead: false,
    })
  );

  // Bob marks it as read
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("conversations").doc(convId).collection("messages").doc("msg1").update({
      isRead: true,
      readAt: new Date(),
    })
  );
});
