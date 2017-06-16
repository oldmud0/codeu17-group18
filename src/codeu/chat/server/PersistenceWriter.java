package codeu.chat.server;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.stream.JsonWriter;

import codeu.chat.common.ConversationHeader;
import codeu.chat.common.ConversationPayload;
import codeu.chat.common.Message;
import codeu.chat.common.OmniView;
import codeu.chat.common.User;
import codeu.chat.util.Logger;
import codeu.chat.util.Time;
import codeu.chat.util.Uuid;

/**
 * Encapsulates a process of writing a persistence file to disk.
 * 
 * <p>A persistence file includes the server's state, including:
 * 
 * <p><ul>
 * <li>Server UUID
 * <li>Shared secret between server and relays
 * <li>Last seen bundle ID from relay
 * <li>Server's last used version
 * </ul>
 * 
 * <p>Other data present in the model, such as users, conversations, and messages
 * are included in the persistence file in a logical order.
 * 
 * <p>The above data is serialized into JSON and written periodically to disk.
 * 
 * @see PersistenceWriterRunnable
 *
 */
public class PersistenceWriter {

  private static final Logger.Log LOG = Logger.newLog(PersistenceWriter.class);

  /** The file to be written. */
  private final File file;

  /** The view used to read the data model. */
  private final OmniView view;

  /** Some persistence info that is private to the server implementation. */
  private final PersistenceFileSkeleton.ServerInfo serverInfo;

  /** The file skeleton that will be used for serialization. */
  private final PersistenceFileSkeleton fileSkeleton = new PersistenceFileSkeleton() {

    @Override
    public ServerInfo serverInfo() {
      return serverInfo;
    }

    @Override
    public Map<Uuid, User> users() {
      Collection<User> users = view.getUsers();
      Map<Uuid, User> map = new HashMap<Uuid, User>();
      for (final User user : users) {
        map.put(user.id, user);
      }
      return map;
    }

    @Override
    public Map<Uuid, ConversationHeader> conversationHeaders() {
      Collection<ConversationHeader> conversations = view.getConversations();
      Map<Uuid, ConversationHeader> map = new HashMap<Uuid, ConversationHeader>();
      for (final ConversationHeader conversation : conversations) {
        map.put(conversation.id, conversation);
      }
      return map;
    }

    @Override
    public Map<Uuid, ConversationPayload> conversationPayloads() {
      Collection<ConversationPayload> payloads = view.getConversationPayloads();
      Map<Uuid, ConversationPayload> map = new HashMap<Uuid, ConversationPayload>();
      for (final ConversationPayload payload : payloads) {
        map.put(payload.id, payload);
      }
      return map;
    }

    @Override
    public Map<Uuid, Message> messages() {
      Collection<Message> messages = view.getMessages();
      Map<Uuid, Message> map = new HashMap<Uuid, Message>();
      for (final Message message : messages) {
        map.put(message.id, message);
      }
      return map;
    }

  };

  public PersistenceWriter(File file, OmniView view, PersistenceFileSkeleton.ServerInfo serverInfo) {
    this.file = file;
    this.view = view;
    this.serverInfo = serverInfo;
  }

  /**
   * Captures the server state and writes the persistence file immediately.
   */
  public void write() throws IOException {
    Gson gson = new GsonBuilder()
        .setDateFormat(DateFormat.LONG)
        .serializeNulls()
        .setPrettyPrinting()
        .registerTypeAdapter(PersistenceFileSkeleton.class,
            new AnnotatedSerializer<PersistenceFileSkeleton>(PersistenceFileSkeleton.class))
        .registerTypeAdapter(PersistenceFileSkeleton.ServerInfo.class,
            new AnnotatedSerializer<PersistenceFileSkeleton.ServerInfo>(PersistenceFileSkeleton.ServerInfo.class))
        .registerTypeAdapter(Uuid.class, new UuidSerializer())
        .registerTypeAdapter(Time.class, new TimeSerializer())
        .create();

    JsonWriter writer = gson.newJsonWriter(new FileWriter(file));
    gson.toJson(fileSkeleton, PersistenceFileSkeleton.class, writer);
    writer.close();
  }

  private class AnnotatedSerializer<T> implements JsonSerializer<T> {

    private final Class<T> genericType;

    public AnnotatedSerializer(Class<T> genericType) {
      this.genericType = genericType;
    }

    @Override
    public JsonElement serialize(T source, Type typeOfSource, JsonSerializationContext context) {
      final JsonObject root = new JsonObject();

      Method[] methods = genericType.getMethods();

      for (Method method : methods) {
        Type returnType = method.getGenericReturnType();
        JsonProperty jsonProperty = (JsonProperty) method.getAnnotation(JsonProperty.class);
        String propertyName = jsonProperty.value();
        LOG.verbose("Method name: %s, returns %s%n", method.getName(), returnType);

        try {
          root.add(propertyName, context.serialize(method.invoke(source), returnType));
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
          LOG.error(e, "Error serializing property %s", method.getName());
        }
      }
      return root;
    }
  }

  private class UuidSerializer implements JsonSerializer<Uuid> {

    @Override
    public JsonElement serialize(Uuid source, Type typeOfSource, JsonSerializationContext context) {
      return context.serialize(source.toString(), String.class);
    }
  }

  private class TimeSerializer implements JsonSerializer<Time> {

    @Override
    public JsonElement serialize(Time source, Type typeOfSource, JsonSerializationContext context) {
      return context.serialize(source.inMs(), long.class);
    }
  }
}
