package codeu.chat.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import codeu.chat.common.ConversationHeader;
import codeu.chat.common.ConversationPayload;
import codeu.chat.common.Message;
import codeu.chat.common.User;
import codeu.chat.server.PersistenceFileSkeleton.ServerInfo;
import codeu.chat.util.Time;
import codeu.chat.util.Uuid;

public class PersistenceReader {
	JsonReader reader;
	GsonBuilder gsonBuilder;
	Gson gson;

	public PersistenceReader(File persistenceFile) {
		gsonBuilder = new GsonBuilder();
		gsonBuilder
			.registerTypeAdapter(ServerInfo.class, new ServerInfoDeserializer())
			.registerTypeAdapter(ConversationHeader.class, new ConversationHeaderDeserializer())
			.registerTypeAdapter(ConversationPayload.class, new ConversationPayloadDeserializer())
			.registerTypeAdapter(Message.class, new MessageDeserializer())
			.registerTypeAdapter(User.class, new UserDeserializer());
		gson = gsonBuilder.create();
	}

	public void read() throws IOException {

	}

	private class ServerInfoDeserializer implements JsonDeserializer<ServerInfo> {

		@Override
		public ServerInfo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			final JsonObject jsonObject = json.getAsJsonObject();

			final int uuid = jsonObject.get("uuid").getAsInt();
			// getting the bytes that make up the Secret in an array
			final JsonArray jsonSecretBytes = jsonObject.getAsJsonArray("bytes");
			final int[] secretBytes = new int[jsonSecretBytes.size()];
			for (int i = 0; i < secretBytes.length; i++) {
				final JsonElement jsonByte = jsonSecretBytes.get(i);
				secretBytes[i] = jsonByte.getAsInt();
			}

			final String jsonLastSeen = jsonObject.get("last_seen").getAsString();
			
			final Uuid serverUuid = new Uuid(uuid);

			// create an instance of ServerInfo, set required fields to the
			// values given by the json file, and return

			return null;
		}
	}

	private class ConversationHeaderDeserializer implements JsonDeserializer<ConversationHeader> {

		@Override
		public ConversationHeader deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			final JsonObject jsonObject = json.getAsJsonObject();
			
			final int id = jsonObject.get("id").getAsInt();
			final int owner = jsonObject.get("owner").getAsInt();
			final long time = jsonObject.get("creation").getAsLong();
			final String title = jsonObject.get("title").getAsString();
			
			final Uuid convoId = new Uuid(id);
			final Uuid ownerId = new Uuid(owner);
			//private method, change to public?
			final Time convoTime = new Time(time);
			
			final ConversationHeader convoResult = new ConversationHeader(convoId, ownerId, convoTime, title);					
			return convoResult;
		}
	}
	
	private class ConversationPayloadDeserializer implements JsonDeserializer<ConversationPayload>{

		@Override
		public ConversationPayload deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			final JsonObject jsonObject = json.getAsJsonObject();
			
			final int id = jsonObject.get("id").getAsInt();
			final int firstMessage = jsonObject.get("firstMessage").getAsInt();
			final int lastMessage = jsonObject.get("lastMessage").getAsInt();
			
			final Uuid convoId = new Uuid(id);
			final Uuid convoFirstMessage = new Uuid(firstMessage);
			final Uuid convoLastMessage = new Uuid(lastMessage);
			
			final ConversationPayload convoResult = new ConversationPayload(convoId, convoFirstMessage, convoLastMessage);
			return convoResult;
		}
	}
	
	private class MessageDeserializer implements JsonDeserializer<Message>{

		@Override
		public Message deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			final JsonObject jsonObject = json.getAsJsonObject();
			
			final int id = jsonObject.get("id").getAsInt();
			final int next = jsonObject.get("next").getAsInt();
			final int previous = jsonObject.get("previous").getAsInt();
			final long time = jsonObject.get("creation").getAsLong();
			final int author = jsonObject.get("author").getAsInt();
			final String content = jsonObject.get("content").getAsString();
			
			final Uuid messageId = new Uuid(id);
			final Uuid nextId = new Uuid(next);
			final Uuid prevId = new Uuid(previous);
			final Time messageTime = new Time(time);
			final Uuid authorId = new Uuid(author);
			
			final Message messageResult = new Message(messageId, nextId, prevId, messageTime, authorId, content);		
			return messageResult;
		}
	}
	
	private class UserDeserializer implements JsonDeserializer<User>{

		@Override
		public User deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			final JsonObject jsonObject = json.getAsJsonObject();
			
			final int id = jsonObject.get("id").getAsInt();
			final String name = jsonObject.get("name").getAsString();
			final long time = jsonObject.get("creation").getAsLong();
			
			final Uuid userId = new Uuid(id);
			final Time userTime = new Time(time);
			
			final User userResult = new User(userId, name, userTime);
			return userResult;
		}
		
	}
}
