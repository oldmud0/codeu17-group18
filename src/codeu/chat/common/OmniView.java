package codeu.chat.common;

import java.util.Collection;

/**
 * A view that allows aggregated retrieval of all objects in the model.
 * 
 * <p>As this mode of access is not ordinary for a user, it is placed aside to be
 * implemented by the {@link codeu.chat.server.Server Server} class (such as for
 * the {@link codeu.chat.server.PersistenceWriter PersistenceWriter}) or another
 * class that necessitates the aggregation of all stored data, such as an
 * application that generates statistics based on the data.
 */
public interface OmniView {

  /**
   * Returns all users.
   * 
   * @see BasicView#getUsers()
   */
  Collection<User> getUsers();

  /**
   * Returns the basic information about all conversations.
   * 
   * @see BasicView#getConversations()
   */
  Collection<ConversationHeader> getConversations();

  /**
   * Returns all conversation payloads (a link to the first and last message of
   * each conversation).
   * 
   * @see BasicView#getConversationPayloads(Collection)
   */
  Collection<ConversationPayload> getConversationPayloads();

  /**
   * Returns all messages.
   * 
   * @see BasicView#getMessages(Collection)
   */
  Collection<Message> getMessages();
}
