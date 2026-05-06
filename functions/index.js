const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");
admin.initializeApp();

function toMillis(value) {
  if (!value) return 0;
  if (typeof value === "number") return value;
  if (typeof value.toMillis === "function") return value.toMillis();
  return 0;
}

exports.sendFriendRequestNotification = onDocumentCreated(
  "friendRequests/{requestId}",
  async (event) => {
    try {
      const snap = event.data;
      if (!snap) return;

      const request = snap.data();
      const senderId = request.fromUserId;
      const recipientId = request.toUserId;

      const senderDoc = await admin.firestore()
        .collection("users")
        .doc(senderId)
        .get();

      const senderData = senderDoc.data();
      const senderName = senderData && senderData.name
        ? senderData.name
        : "Someone";

      const recipientDoc = await admin.firestore()
        .collection("users")
        .doc(recipientId)
        .get();

      const recipientData = recipientDoc.data();
      const fcmToken = recipientData && recipientData.fcmToken
        ? recipientData.fcmToken
        : null;

      if (!fcmToken) return;

      await admin.messaging().send({
        token: fcmToken,
        data: {
          type: "friend_request",
          senderId: senderId,
          requestId: event.params.requestId,
          title: "New Friend Request",
          body: senderName + " sent you a friend request!",
        },
        android: {
          priority: "high",
          notification: {
            channelId: "friend_requests_channel",
            sound: "default",
          },
        },
      });

      console.log("Friend request notification sent to " + recipientId);
    } catch (error) {
      console.error("Error sending friend request notification:", error);
    }
  }
);

exports.sendMessageNotification = onDocumentCreated(
  "conversations/{convId}/messages/{msgId}",
  async (event) => {
    try {
      const snap = event.data;
      if (!snap) return;

      const message = snap.data();
      const senderId = message.senderId;
      const text = message.text || "New message";
      const convId = event.params.convId;

      const senderDoc = await admin.firestore()
        .collection("users")
        .doc(senderId)
        .get();

      const senderData = senderDoc.data();
      const senderName = senderData && senderData.name
        ? senderData.name
        : "Someone";

      const convDoc = await admin.firestore()
        .collection("conversations")
        .doc(convId)
        .get();

      const convData = convDoc.data();
      const participants = convData && convData.members
        ? convData.members
        : [];

      const recipients = participants.filter((uid) => uid !== senderId);
      const activeConversationFreshAfterMs = 2 * 60 * 1000;

      const notifications = recipients.map(async (recipientId) => {
        const recipientDoc = await admin.firestore()
          .collection("users")
          .doc(recipientId)
          .get();

        const recipientData = recipientDoc.data();
        const fcmToken = recipientData && recipientData.fcmToken
          ? recipientData.fcmToken
          : null;
        if (!fcmToken) return;

        // Skip if recipient already has this conversation open
        const activeConversationId = recipientData.activeConversationId || null;
        const activeConversationUpdatedAt = toMillis(
          recipientData.activeConversationUpdatedAt,
        );
        const isViewingConversation =
          activeConversationId === convId &&
          activeConversationUpdatedAt > 0 &&
          Date.now() - activeConversationUpdatedAt <=
            activeConversationFreshAfterMs;

        if (isViewingConversation) {
          console.log(
            "Skipping notification for " + recipientId +
              " - already in conversation",
          );
          return;
        }

        const bodyText = text.length > 100
          ? text.substring(0, 100) + "..."
          : text;

        return admin.messaging().send({
          token: fcmToken,
          data: {
            conversationId: convId,
            senderId: senderId,
            title: senderName,
            body: bodyText,
            type: "message",
          },
          android: {
            priority: "high",
            notification: {
              channelId: "messages_channel",
              sound: "default",
            },
          },
        });
      });

      await Promise.all(notifications);
      console.log("Notifications sent for conversation " + convId);
    } catch (error) {
      console.error("Error sending notification:", error);
    }
  }
);
