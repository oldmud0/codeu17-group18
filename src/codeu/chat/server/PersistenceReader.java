package codeu.chat.server;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;

import codeu.chat.common.ConversationHeader;
import codeu.chat.common.ConversationPayload;
import codeu.chat.common.Message;
import codeu.chat.common.Secret;
import codeu.chat.common.User;
import codeu.chat.common.VersionInfo;
import codeu.chat.util.Logger;
import codeu.chat.util.Time;
import codeu.chat.util.Uuid;

public class PersistenceReader {

  private static final Logger.Log LOG = Logger.newLog(PersistenceReader.class);

  private final File persistenceFile;

  private PersistenceFileSkeleton fileContainer = new PersistenceFileSkeleton() {

    protected ServerInfo serverInfo = new PersistenceFileSkeleton.ServerInfo() {

      protected VersionInfo version;
      protected Secret secret;
      protected Uuid lastSeen;
      protected Uuid id;

      @Override
      public VersionInfo version() {
        return version;
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
      public Uuid id() {
        return id;
      }
    };

    protected Map<Uuid, User> users;
    protected Map<Uuid, ConversationHeader> conversationHeaders;
    protected Map<Uuid, ConversationPayload> conversationPayloads;
    protected Map<Uuid, Message> messages;

    @Override
    public ServerInfo serverInfo() {
      return serverInfo;
    }

    @Override
    public Map<Uuid, User> users() {
      return users;
    }

    @Override
    public Map<Uuid, ConversationHeader> conversationHeaders() {
      return conversationHeaders;
    }

    @Override
    public Map<Uuid, ConversationPayload> conversationPayloads() {
      return conversationPayloads;
    }

    @Override
    public Map<Uuid, Message> messages() {
      return messages;
    }

  };

  public PersistenceReader(File persistenceFile) {
    this.persistenceFile = persistenceFile;
  }

  public void read() throws IOException {
    Gson gson = new GsonBuilder()
        .registerTypeAdapter(PersistenceFileSkeleton.class,
            new AnnotatedDeserializer<PersistenceFileSkeleton>(
                PersistenceFileSkeleton.class, fileContainer))
        .registerTypeAdapter(PersistenceFileSkeleton.ServerInfo.class,
            new AnnotatedDeserializer<PersistenceFileSkeleton.ServerInfo>(
                PersistenceFileSkeleton.ServerInfo.class, fileContainer.serverInfo()))
        .registerTypeAdapter(Uuid.class, new UuidDeserializer())
        .registerTypeAdapter(Time.class, new TimeDeserializer())
        .create();

    JsonReader reader = gson.newJsonReader(new FileReader(persistenceFile));
    fileContainer = gson.fromJson(reader, PersistenceFileSkeleton.class);
  }

  /**
   * Returns a container that contains the data read from the persistence file.
   */
  public PersistenceFileSkeleton getContainer() {
    return fileContainer;
  }

  private class AnnotatedDeserializer<T> implements JsonDeserializer<T> {

    private final Class<T> type;
    private final T target;

    public AnnotatedDeserializer(Class<T> type, T target) {
      this.type = type;
      this.target = target;
    }

    @Override
    public T deserialize(JsonElement source, Type typeOfSource, JsonDeserializationContext context)
        throws JsonParseException {
      JsonObject obj = source.getAsJsonObject();

      Method[] methods = type.getMethods();

      // For each method in the persistence file skeleton...
      // (a method, in this case, represents a root object that will be stored)
      for (Method method : methods) {
        // Get the type of that method and check if it has the JsonProperty
        // annotation.
        Type returnType = method.getGenericReturnType();

        JsonProperty jsonProperty = method.getAnnotation(JsonProperty.class);
        String propertyName = jsonProperty.value();
        LOG.verbose("Method name: %s, returns %s%n", method.getName(), returnType);

        Field methodField;
        try {
          // Now find the field that corresponds to that method in *our*
          // implementation
          // of the PersistenceFileSkeleton.
          methodField = target.getClass().getDeclaredField(method.getName());

          // Assuming the field was found, we'll parse the contents stored in
          // that property name (as dictated by the @JsonProperty annotation) and put those
          // contents right in the field we just found.
          methodField.set(target, context.deserialize(obj.get(propertyName), returnType));
        } catch (NoSuchFieldException | SecurityException | IllegalAccessException | IllegalArgumentException e) {
          // In case we didn't find any field to put our data into, we will not
          // deserialize this property.
          LOG.error(e, "Error deserializing property %s", method.getName());
        }
      }
      return target;
    }

  }

  private class UuidDeserializer implements JsonDeserializer<Uuid> {

    @Override
    public Uuid deserialize(JsonElement source, Type typeOfSource, JsonDeserializationContext context)
        throws JsonParseException {
      try {
        return Uuid.parse(source.getAsString());
      } catch (IOException e) {
        LOG.error(e, "Error deserializing UUID %s. Using Uuid.NULL instead.", source.getAsString());
        return Uuid.NULL;
      }
    }

  }

  private class TimeDeserializer implements JsonDeserializer<Time> {

    @Override
    public Time deserialize(JsonElement source, Type typeOfSource, JsonDeserializationContext context)
        throws JsonParseException {
      return Time.fromMs(source.getAsLong());
    }

  }
}
