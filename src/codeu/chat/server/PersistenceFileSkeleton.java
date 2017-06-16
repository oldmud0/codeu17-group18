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

  @JsonProperty("server_info")
  ServerInfo serverInfo();

  @JsonProperty("users")
  Map<Uuid, User> users();

  @JsonProperty("conversation_headers")
  Map<Uuid, ConversationHeader> conversationHeaders();

  @JsonProperty("conversation_payloads")
  Map<Uuid, ConversationPayload> conversationPayloads();

  @JsonProperty("messages")
  Map<Uuid, Message> messages();

}
