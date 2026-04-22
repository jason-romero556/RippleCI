const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");
admin.initializeApp();

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

      // Get sender's display name
      const senderDoc = await admin.firestore()
        .collection("users")
        .doc(senderId)
        .get();

      const senderData = senderDoc.data();
      const senderName = senderData && senderData.name
        ? senderData.name
        : "Someone";

      // Get conversation participants
      const convDoc = await admin.firestore()
        .collection("conversations")
        .doc(convId)
        .get();

      const convData = convDoc.data();
      const participants = convData && convData.members
        ? convData.members
        : [];

      // Send to everyone except sender
      const recipients = participants.filter((uid) => uid !== senderId);

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

        const bodyText = text.length > 100
          ? text.substring(0, 100) + "..."
          : text;

        return admin.messaging().send({
          token: fcmToken,
          notification: {
            title: senderName,
            body: bodyText,
          },
          data: {
            conversationId: convId,
            senderId: senderId,
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