// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package codeu.chat.server;

import codeu.chat.common.BasicController;
import codeu.chat.common.ConversationHeader;
import codeu.chat.common.ConversationPayload;
import codeu.chat.common.Message;
import codeu.chat.common.RandomUuidGenerator;
import codeu.chat.common.RawController;
import codeu.chat.common.User;
import codeu.chat.security.ConversationSecurityDescriptor;
import codeu.chat.security.SecurityViolationException;
import codeu.chat.util.Logger;
import codeu.chat.util.Time;
import codeu.chat.util.Uuid;
import codeu.chat.util.store.StoreAccessor;

public final class Controller implements RawController, BasicController {

  private final static Logger.Log LOG = Logger.newLog(Controller.class);

  private final Model model;
  private final Uuid.Generator uuidGenerator;

  public Controller(Uuid serverId, Model model) {
    this.model = model;
    this.uuidGenerator = new RandomUuidGenerator(serverId, System.currentTimeMillis());
  }

  @Override
  public Message newMessage(Uuid author, Uuid conversation, String body) {
    return newMessage(createId(), author, conversation, body, Time.now());
  }

  @Override
  public User newUser(String name) {
    return newUser(createId(), name, Time.now());
  }

  @Override
  public ConversationHeader newConversation(String title, Uuid owner) {
    return newConversation(createId(), title, owner, Time.now());
  }

  @Override
  public Message newMessage(Uuid id, Uuid author, Uuid conversation, String body, Time creationTime) {

    final User foundUser = model.userById().first(author);
    final ConversationPayload foundConversation = model.conversationPayloadById().first(conversation);

    Message message = null;

    if (foundUser != null && foundConversation != null && isIdFree(id)) {

      message = new Message(id, Uuid.NULL, Uuid.NULL, creationTime, author, body);
      model.add(message);
      LOG.info("Message added: %s", message.id);

      // Find and update the previous "last" message so that it's "next" value
      // will point to the new message.

      if (Uuid.equals(foundConversation.lastMessage, Uuid.NULL)) {

        // The conversation has no messages in it, that's why the last message is NULL (the first
        // message should be NULL too. Since there is no last message, then it is not possible
        // to update the last message's "next" value.

      } else {
        final Message lastMessage = model.messageById().first(foundConversation.lastMessage);
        lastMessage.next = message.id;
      }

      // If the first message points to NULL it means that the conversation was empty and that
      // the first message should be set to the new message. Otherwise the message should
      // not change.

      foundConversation.firstMessage =
          Uuid.equals(foundConversation.firstMessage, Uuid.NULL) ?
          message.id :
          foundConversation.firstMessage;

      // Update the conversation to point to the new last message as it has changed.

      foundConversation.lastMessage = message.id;
    }

    return message;
  }

  @Override
  public User newUser(Uuid id, String name, Time creationTime) {

    User user = null;

    if (isIdFree(id)) {

      user = new User(id, name, creationTime);
      model.add(user);

      LOG.info(
          "newUser success (user.id=%s user.name=%s user.time=%s)",
          id,
          name,
          creationTime);

    } else {

      LOG.info(
          "newUser fail - id in use (user.id=%s user.name=%s user.time=%s)",
          id,
          name,
          creationTime);
    }

    return user;
  }

  @Override
  public String newUserInterest(String name, Uuid signedInId) {
    String str = "I am in the controller";
    return str;
  }

  @Override
  public String newConvoInterest(String name, Uuid owner){
    String str = "I am convo interest in the controller";
    return str;
  }
  @Override
  public String deleteUserInterest(String name, Uuid signedInId) {
    String str = "";
    return str;
  }

  @Override
  public String deleteConvoInterest(String name, Uuid signedInId) {
    String str = "";
    return str;
  }

  @Override
  public void setConversationExplicitPermissions(Uuid conversationId, Uuid invokerId, Uuid targetId, int flags)
  		throws SecurityViolationException {
  	ConversationHeader header = model.conversationById().first(conversationId);
  	ConversationSecurityDescriptor descrip = header.security;
  	descrip.setPermissions(invokerId, targetId, flags);
  }

  @Override
  public void deleteMessage(Uuid conversationId, Uuid messageId) throws SecurityViolationException {
    ConversationPayload messages = model.conversationPayloadById().first(conversationId);
    StoreAccessor<Uuid, Message> accessor = model.messageById();

    // Check if we are deleting the first message
    if (messageId.equals(messages.firstMessage)) {
      messages.firstMessage = accessor.first(messageId).next;
      model.remove(accessor.first(messageId));

      // What if it is the first and the last message?
      // Then there are no more messages in the conversation.
      if (messageId.equals(messages.lastMessage)) {
        messages.lastMessage = Uuid.NULL;
      }
      return;
    }

    for (Message msg = accessor.first(messages.firstMessage); msg.next != null; msg = accessor.first(msg.next)) {
      // Find the message previous to the message we want to remove
      Message next = accessor.first(msg.next);
      if (next.id.equals(messageId)) {
        // next is our target message, in message form
        // Now relink our current msg so that it hops over the message we are to remove
        msg.next = next.next;

        // If there is a message after the message to delete, set its previous to the
        // current message
        if (!next.next.equals(Uuid.NULL)) {
          accessor.first(next.next).previous = msg.id;
        }

        // Check if deleted message was the last message
        if (messageId.equals(messages.lastMessage)) {
          messages.lastMessage = msg.id;
        }

        model.remove(next);
        return;
      }
    }
  }

  @Override
  public void deleteConversation(Uuid conversationId) throws SecurityViolationException {
    StoreAccessor<Uuid, ConversationHeader> accessor = model.conversationById();
    ConversationPayload messages = model.conversationPayloadById().first(conversationId);
    model.remove(accessor.first(conversationId));
    StoreAccessor<Uuid, Message> messageAccessor = model.messageById();
    for (Message msg = messageAccessor.first(messages.firstMessage); msg.next != null; msg = messageAccessor.first(msg.next)) {
      model.remove(msg);
    }
  }

  @Override
  public ConversationHeader newConversation(Uuid id, String title, Uuid owner, Time creationTime) {

    final User foundOwner = model.userById().first(owner);

    ConversationHeader conversation = null;

    if (foundOwner != null && isIdFree(id)) {
      conversation = new ConversationHeader(id, owner, creationTime, title);
      model.add(conversation);
      LOG.info("Conversation added: " + id);
    }

    return conversation;
  }

  private Uuid createId() {

    Uuid candidate;

    for (candidate = uuidGenerator.make();
         isIdInUse(candidate);
         candidate = uuidGenerator.make()) {

     // Assuming that "randomUuid" is actually well implemented, this
     // loop should never be needed, but just incase make sure that the
     // Uuid is not actually in use before returning it.

    }

    return candidate;
  }

  private boolean isIdInUse(Uuid id) {
    return model.messageById().first(id) != null ||
           model.conversationById().first(id) != null ||
           model.userById().first(id) != null;
  }

  private boolean isIdFree(Uuid id) { return !isIdInUse(id); }

}
