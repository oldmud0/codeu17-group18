package codeu.chat.server;

import java.util.Map;

import codeu.chat.common.ConversationHeader;
import codeu.chat.common.ConversationPayload;
import codeu.chat.common.Message;
import codeu.chat.common.Secret;
import codeu.chat.common.User;
import codeu.chat.util.Uuid;

public interface PersistenceFileSkeleton {
  public interface ServerInfo {
    Uuid id();

    Secret secret();

    Uuid lastSeen();
  }

  ServerInfo serverInfo();

  Map<Uuid, User> users();

  Map<Uuid, ConversationHeader> conversationHeaders();

  Map<Uuid, ConversationPayload> conversationPayloads();

  Map<Uuid, Message> messages();

}
