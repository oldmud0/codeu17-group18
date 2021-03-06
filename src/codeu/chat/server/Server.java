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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import codeu.chat.common.ConversationHeader;
import codeu.chat.common.ConversationPayload;
import codeu.chat.common.Message;
import codeu.chat.common.NetworkCode;
import codeu.chat.common.Relay;
import codeu.chat.common.Secret;
import codeu.chat.common.User;
import codeu.chat.common.VersionInfo;
import codeu.chat.security.SecurityViolationException;
import codeu.chat.server.PersistenceFileSkeleton.ServerInfo;
import codeu.chat.server.contexts.ConversationContext;
import codeu.chat.server.contexts.UserContext;
import codeu.chat.util.InterestInfo;
import codeu.chat.util.Logger;
import codeu.chat.util.Serializers;
import codeu.chat.util.Time;
import codeu.chat.util.Timeline;
import codeu.chat.util.Uuid;
import codeu.chat.util.connections.Connection;

public final class Server {

  private interface Command {
    void onMessage(InputStream in, OutputStream out) throws IOException;
  }

  private static final Logger.Log LOG = Logger.newLog(Server.class);

  private static final int RELAY_REFRESH_MS = 5000; // 5 seconds

  private final Timeline timeline = new Timeline();

  private final Map<Integer, Command> commands = new HashMap<>();
  // all the users in the chat app and their interest info
  private Map<User, InterestInfo> userInterests = new HashMap<>();
  private final Uuid id;
  private final Secret secret;

  private final Model model = new Model();
  private final View view = new View(model);
  private final Controller controller;

  private final Relay relay;
  private Uuid lastSeen = Uuid.NULL;

  private PersistenceWriter persistenceWriter; // Not final, as it is not required

  private final VersionInfo version = new VersionInfo();
  private static final codeu.chat.util.ServerInfo info = new codeu.chat.util.ServerInfo();

  public Server(final Uuid id, final Secret secret, final Relay relay) {

    this.id = id;
    this.secret = secret;
    this.controller = new Controller(id, model);
    this.relay = relay;

    // New Message - A client wants to add a new message to the back end.
    this.commands.put(NetworkCode.NEW_MESSAGE_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {
        final Uuid author = Uuid.SERIALIZER.read(in);
        User signedInUser = view.findUser(author);
        final Uuid conversation = Uuid.SERIALIZER.read(in);
        final String content = Serializers.STRING.read(in);
        ConversationHeader convo = view.findConversation(conversation);
        ConversationContext conversationContext = new ConversationContext(signedInUser, convo, view, controller);

        try {
          final codeu.chat.contexts.MessageContext msgContext = conversationContext.add(content);
          final Message message = msgContext.message;

          // for user status update
          userInterests.get(signedInUser).addModifiedConversation(convo.title);
          // for convo status update
          for (User temp : userInterests.keySet()) {
            if (userInterests.get(temp).getInterestedConvos().isEmpty() == false) {
              if (userInterests.get(temp).getInterestedConvos().containsKey(convo.title)) {
                userInterests.get(temp).addToMessageCount(convo.title);
              }
            }
          }

          Serializers.INTEGER.write(out, NetworkCode.NEW_MESSAGE_RESPONSE);
          Serializers.nullable(Message.SERIALIZER).write(out, message);

          timeline.scheduleNow(createSendToRelayEvent(author, conversation, message.id));
        } catch (SecurityViolationException e) {
          LOG.error(e, "Security violation occured by user: " + signedInUser.name);
          Serializers.INTEGER.write(out, NetworkCode.ERR_SECURITY_VIOLATION);
        }
      }
    });

    // New User - A client wants to add a new user to the back end.
    this.commands.put(NetworkCode.NEW_USER_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {

        final String name = Serializers.STRING.read(in);
        final User user = controller.newUser(name);
        userInterests.put(user, new InterestInfo());

        Serializers.INTEGER.write(out, NetworkCode.NEW_USER_RESPONSE);
        Serializers.nullable(User.SERIALIZER).write(out, user);
      }
    });

    // New Conversation - A client wants to add a new conversation to the
    // back end.
    this.commands.put(NetworkCode.NEW_CONVERSATION_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {
        // title of conversation
        final String title = Serializers.STRING.read(in);
        // user that we put in as a key
        final Uuid owner = Uuid.SERIALIZER.read(in);
        User signedInUser = view.findUser(owner);
        userInterests.get(signedInUser).addModifiedConversation(title);
        // userInterests.get(owner).addInterestConvo(title);
        final ConversationHeader conversation = controller.newConversation(title, owner);

        Serializers.INTEGER.write(out, NetworkCode.NEW_CONVERSATION_RESPONSE);
        Serializers.nullable(ConversationHeader.SERIALIZER).write(out, conversation);
      }
    });

    // Get Users - A client wants to get all the users from the back end.
    this.commands.put(NetworkCode.GET_USERS_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {

        final Collection<User> users = view.getUsers();

        Serializers.INTEGER.write(out, NetworkCode.GET_USERS_RESPONSE);
        Serializers.collection(User.SERIALIZER).write(out, users);
      }
    });

    // Get Conversations - A client wants to get all the conversations from
    // the back end.
    this.commands.put(NetworkCode.GET_ALL_CONVERSATIONS_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {
        final Uuid userId = Uuid.SERIALIZER.read(in);
        final UserContext user = new UserContext(view.findUser(userId), view, controller);
        final Collection<ConversationHeader> conversations = new ArrayList<>(); 
        final Iterable<codeu.chat.contexts.ConversationContext> conversationContexts = user.conversations();
        for (codeu.chat.contexts.ConversationContext context : conversationContexts) {
          conversations.add(context.conversation);
        }

        Serializers.INTEGER.write(out, NetworkCode.GET_ALL_CONVERSATIONS_RESPONSE);
        Serializers.collection(ConversationHeader.SERIALIZER).write(out, conversations);
      }
    });

    // Get Conversations By Id - A client wants to get a subset of the
    // converations from
    // the back end. Normally this will be done after calling
    // Get Conversations to get all the headers and now the client
    // wants to get a subset of the payloads.
    this.commands.put(NetworkCode.GET_CONVERSATIONS_BY_ID_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {
        final Uuid userId = Uuid.SERIALIZER.read(in);

        // XXX: make UserContext work with the user id, so that a security check is available here.
        // The problem is that this operation is inherently indirect and dubious in motive.
        // On the client side, there is a totally different approach that is taken to get here.

        //final UserContext user = new UserContext(view.findUser(userId), view, controller);
        final Collection<Uuid> ids = Serializers.collection(Uuid.SERIALIZER).read(in);
        final Collection<ConversationPayload> conversations = view.getConversationPayloads(ids);

        Serializers.INTEGER.write(out, NetworkCode.GET_CONVERSATIONS_BY_ID_RESPONSE);
        Serializers.collection(ConversationPayload.SERIALIZER).write(out, conversations);
      }
    });

    // Get Messages By Id - A client wants to get a subset of the messages
    // from the back end.
    this.commands.put(NetworkCode.GET_MESSAGES_BY_ID_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {

        final Collection<Uuid> ids = Serializers.collection(Uuid.SERIALIZER).read(in);
        final Collection<Message> messages = view.getMessages(ids);

        Serializers.INTEGER.write(out, NetworkCode.GET_MESSAGES_BY_ID_RESPONSE);
        Serializers.collection(Message.SERIALIZER).write(out, messages);
      }
    });

    this.commands.put(NetworkCode.GET_SERVER_VERSION_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {
        Serializers.INTEGER.write(out, NetworkCode.GET_SERVER_VERSION_RESPONSE);
        Uuid.SERIALIZER.write(out, view.getVersion().version);
      }
    });

    this.commands.put(NetworkCode.SERVER_INFO_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {
        Serializers.INTEGER.write(out, NetworkCode.SERVER_INFO_RESPONSE);
        Time.SERIALIZER.write(out, view.getInfo().startTime);
      }
    });

    this.commands.put(NetworkCode.NEW_USER_INTEREST_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {
        User interest = null;
        final String interestName = Serializers.STRING.read(in);

        for(User temp : userInterests.keySet()) {
          if(temp.name.equals(interestName)) {
            interest = temp;
          }
        }

        Uuid interestID = interest.id;
        final Uuid signedInId = Uuid.SERIALIZER.read(in);
        User signedInUser = view.findUser(signedInId);
        User temp = null;

        for (User key : userInterests.keySet()) {
          if(key.equals(signedInUser)){
            userInterests.get(key).addInterestUser(interestID);
            temp = key;
          }
        }

        String confirmation = new String("You have added "+ '"' + interestName + '"' + " to your interests, congratulations.");
        Serializers.INTEGER.write(out, NetworkCode.NEW_USER_INTEREST_RESPONSE);
        Serializers.STRING.write(out, confirmation);
      }
    });

    this.commands.put(NetworkCode.GET_USER_STATUS_UPDATE_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {
        final Uuid signedInId = Uuid.SERIALIZER.read(in);
        final User signedInUser = view.findUser(signedInId);
        User temp = null;

        for (User key : userInterests.keySet()) {
          if (key.equals(signedInUser)) {
            temp = key;
          }
        }

        final Set<Uuid> ids = userInterests.get(temp).getInterestedUserIds();
        Set<String> uniqueConvos = new HashSet<String>();

        for (Uuid interestId : ids) {
          User interestUser = view.findUser(interestId);
          uniqueConvos.addAll(userInterests.get(interestUser).getModifiedConvos());
          userInterests.get(interestUser).resetConvos();
        }

        // Java 8 only
        String makeString = String.join(", ", uniqueConvos);

        Serializers.INTEGER.write(out, NetworkCode.GET_USER_STATUS_UPDATE_RESPONSE);
        Serializers.STRING.write(out, makeString + "\n");
      }
    });

    this.commands.put(NetworkCode.NEW_CONVO_INTEREST_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {
        // title of interested convo to be added
        final String title = Serializers.STRING.read(in);
        // signed in user
        final Uuid ownerId = Uuid.SERIALIZER.read(in);
        final User owner = view.findUser(ownerId);
        userInterests.get(owner).addInterestConvo(title);
        String confirmation = new String("You have added " + '"' + title + '"' + " to your interests.");
        Serializers.INTEGER.write(out, NetworkCode.NEW_CONVO_INTEREST_RESPONSE);
        Serializers.STRING.write(out, confirmation);
        // Serializers.nullable(User.SERIALIZER).write(out, user);
      }
    });

    this.commands.put(NetworkCode.REMOVE_USER_INTEREST_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {
        User interest = null;
        final String interestName = Serializers.STRING.read(in);
        for (User temp : userInterests.keySet()) {
          if (temp.name.equals(interestName)) {
            interest = temp;
          }
        }

        Uuid interestID = interest.id;

        final Uuid signedInId = Uuid.SERIALIZER.read(in);
        final User signedInUser = view.findUser(signedInId);

        userInterests.get(signedInUser).removeInterestUser(interestID);

        String confirmation = new String(
            "You have removed the user " + '"' + interestName + '"' + " from your interests.");
        Serializers.INTEGER.write(out, NetworkCode.REMOVE_USER_INTEREST_RESPONSE);
        Serializers.STRING.write(out, confirmation);
      }
    });

    this.commands.put(NetworkCode.REMOVE_CONVO_INTEREST_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {
        // title of interested convo to be added
        final String title = Serializers.STRING.read(in);
        // signed in user
        final Uuid signedInId = Uuid.SERIALIZER.read(in);
        final User signedInUser = view.findUser(signedInId);

        userInterests.get(signedInUser).removeInterestConvo(title);

        String confirmation = new String(
            "You have removed the conversation " + '"' + title + '"' + " from your interests.");
        Serializers.INTEGER.write(out, NetworkCode.REMOVE_CONVO_INTEREST_RESPONSE);
        Serializers.STRING.write(out, confirmation);
      }
    });

    // get a status update for a convo
    this.commands.put(NetworkCode.GET_CONVO_STATUS_UPDATE_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {
        final Uuid signedInId = Uuid.SERIALIZER.read(in);
        final User signedInUser = view.findUser(signedInId);

        String convoStatusUpdate = "";
        Map<String, Integer> interestedConvos = userInterests.get(signedInUser).getInterestedConvos();
        Set<String> convoInterestTitles = interestedConvos.keySet();
        for (String convoTitle : convoInterestTitles) {
          convoStatusUpdate = convoStatusUpdate + convoTitle + " : " + interestedConvos.get(convoTitle) + " ";
          userInterests.get(signedInUser).resetMessages(convoTitle);
        }
        Serializers.INTEGER.write(out, NetworkCode.GET_CONVO_STATUS_UPDATE_RESPONSE);
        Serializers.STRING.write(out, convoStatusUpdate);
      }
    });
    this.commands.put(NetworkCode.NEW_ACCESS_CONTROL_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {
        final Uuid convoId = Uuid.SERIALIZER.read(in);
        final ConversationHeader convoHeader = view.findConversation(convoId);
        final Uuid invokerID = Uuid.SERIALIZER.read(in);
        final User invokerUser = view.findUser(invokerID);
        final Uuid targetId = Uuid.SERIALIZER.read(in);
        final int flag = Serializers.INTEGER.read(in);
        ConversationContext invokerContext = new ConversationContext(invokerUser, convoHeader, view, controller);
        try {
          invokerContext.setSecurityFlags(targetId, flag);
          Serializers.INTEGER.write(out, NetworkCode.NEW_ACCESS_CONTROL_RESPONSE);
        } catch (SecurityViolationException e) {
          LOG.error(e, "Security violation occured by user: " + invokerUser.name);
          Serializers.INTEGER.write(out, NetworkCode.ERR_SECURITY_VIOLATION);
        }
      }
    });
    this.commands.put(NetworkCode.DELETE_MESSAGE_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {
          final Uuid convoId = Uuid.SERIALIZER.read(in);
          final ConversationHeader convoHeader = view.findConversation(convoId);
          final Uuid messageId = Uuid.SERIALIZER.read(in);
          final Uuid invokerId = Uuid.SERIALIZER.read(in);
          final User invokerUser = view.findUser(invokerId);
          ConversationContext invokerContext = new ConversationContext(invokerUser, convoHeader, view, controller);
          try {
              invokerContext.remove(messageId);
              Serializers.INTEGER.write(out, NetworkCode.DELETE_MESSAGE_RESPONSE);
            } catch (SecurityViolationException e) {
              LOG.error(e, "Security violation occured by user: " + invokerUser.name);
              Serializers.INTEGER.write(out, NetworkCode.ERR_SECURITY_VIOLATION);
            }
      }
    });
    this.commands.put(NetworkCode.DELETE_CONVERSATION_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {
          final Uuid convoId = Uuid.SERIALIZER.read(in);
          final Uuid invokerId = Uuid.SERIALIZER.read(in);
          final User invokerUser = view.findUser(invokerId);
          UserContext invokerContext = new UserContext(invokerUser, view, controller);
          try {
              invokerContext.deleteConversation(convoId);
              Serializers.INTEGER.write(out, NetworkCode.DELETE_CONVERSATION_RESPONSE);
            } catch (SecurityViolationException e) {
              LOG.error(e, "Security violation occured by user: " + invokerUser.name);
              Serializers.INTEGER.write(out, NetworkCode.ERR_SECURITY_VIOLATION);
            }
      }
    });
    this.timeline.scheduleNow(new Runnable() {
      @Override
      public void run() {
        try {

          LOG.verbose("Reading update from relay...");

          for (final Relay.Bundle bundle : relay.read(id, secret, lastSeen, 32)) {
            onBundle(bundle);
            lastSeen = bundle.id();
          }

        } catch (Exception ex) {

          LOG.error(ex, "Failed to read update from relay.");

        }

        timeline.scheduleIn(RELAY_REFRESH_MS, this);
      }
    });
  }

  public Server(final Uuid id, final Secret secret, final Relay relay, final File persistenceFile) {
    this(id, secret, relay);

    this.persistenceWriter = new PersistenceWriter(persistenceFile, view, new ServerInfo() {

      @Override
      public Uuid id() {
        return id;
      }

      @Override
      public Secret secret() {
        return secret;
      }

      @Override
      public Uuid lastSeen() {
        return lastSeen;
      }

      @Override
      public VersionInfo version() {
        return version;
      }

    });

    this.timeline.scheduleIn(PersistenceWriterRunnable.WRITE_INTERVAL_MS,
        new PersistenceWriterRunnable(persistenceWriter, timeline));
  }

  public Server(final PersistenceFileSkeleton container, final Relay relay, final File persistenceFile) {
    this(container.serverInfo().id(), container.serverInfo().secret(), relay, persistenceFile);
    lastSeen = container.serverInfo().lastSeen();
    // XXX: version is not written!
    adaptToModel(container);
  }

  private void adaptToModel(PersistenceFileSkeleton container) {
    for (User user : container.users().values()) {
      model.add(user);
    }

    Map<Uuid, ConversationPayload> payloads = container.conversationPayloads();
    for (ConversationHeader conv : container.conversationHeaders().values()) {
      ConversationPayload payload = payloads.get(conv.id);
      model.add(conv, payload);
    }

    for (Message msg : container.messages().values()) {
      model.add(msg);
    }
  }

  public void handleConnection(final Connection connection) {
    timeline.scheduleNow(new Runnable() {
      @Override
      public void run() {
        try {

          LOG.info("Handling connection...");

          final int type = Serializers.INTEGER.read(connection.in());
          final Command command = commands.get(type);
          if (command == null) {
            // The message type cannot be handled so return a dummy
            // message.
            Serializers.INTEGER.write(connection.out(), NetworkCode.NO_MESSAGE);
            LOG.info("Connection rejected");
          } else {
            command.onMessage(connection.in(), connection.out());
            LOG.info("Connection accepted");
          }
        } catch (Exception ex) {

          LOG.error(ex, "Exception while handling connection.");

        }

        try {
          connection.close();
        } catch (Exception ex) {
          LOG.error(ex, "Exception while closing connection.");
        }
      }
    });
  }

  private void onBundle(Relay.Bundle bundle) {

    final Relay.Bundle.Component relayUser = bundle.user();
    final Relay.Bundle.Component relayConversation = bundle.conversation();
    final Relay.Bundle.Component relayMessage = bundle.user();

    User user = model.userById().first(relayUser.id());

    if (user == null) {
      user = controller.newUser(relayUser.id(), relayUser.text(), relayUser.time());
    }

    ConversationHeader conversation = model.conversationById().first(relayConversation.id());

    if (conversation == null) {

      // As the relay does not tell us who made the conversation - the
      // first person who
      // has a message in the conversation will get ownership over this
      // server's copy
      // of the conversation.
      conversation = controller.newConversation(relayConversation.id(), relayConversation.text(), user.id,
          relayConversation.time());
    }

    Message message = model.messageById().first(relayMessage.id());

    if (message == null) {
      message = controller.newMessage(relayMessage.id(), user.id, conversation.id, relayMessage.text(),
          relayMessage.time());
    }
  }

  private Runnable createSendToRelayEvent(final Uuid userId, final Uuid conversationId, final Uuid messageId) {
    return new Runnable() {
      @Override
      public void run() {
        final User user = view.findUser(userId);
        final ConversationHeader conversation = view.findConversation(conversationId);
        final Message message = view.findMessage(messageId);
        relay.write(id, secret, relay.pack(user.id, user.name, user.creation),
            relay.pack(conversation.id, conversation.title, conversation.creation),
            relay.pack(message.id, message.content, message.creation));
      }
    };
  }
}
