package codeu.chat.util;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import codeu.chat.util.Uuid;


public class InterestInfo {
  public static final Serializer<InterestInfo> SERIALIZER = new Serializer<InterestInfo>() {

    @Override
    public void write(OutputStream out, InterestInfo value) throws IOException {

      Serializers.STRING.write(out, value.convos.toString());

    }

    @Override
    public InterestInfo read(InputStream in) throws IOException {

      return new InterestInfo();
    }
  };

    //set of interests that are users
    private Set<Uuid> ids;
    private Map<String, Integer> messagesInConvos;
    private List<String> convos;

    public InterestInfo() {
      ids = new HashSet<Uuid>();
      convos = new ArrayList<>();
      messagesInConvos = new HashMap<String, Integer>();
    }

    // returns a set of interested users
    public Set<Uuid> getInterestedUserIds() {
      return ids;
    }

    // returns a map if interested convo ids mapped to the number of convos in
    // messages
    public Map<String, Integer> getInterestedConvoIds() {
      return messagesInConvos;
    }
    // add interested user
    public void addInterestUser(Uuid newId) {
      ids.add(newId);
    }

    // add interested convo
    public void addInterestConvo(String title) {
      messagesInConvos.put(title, 0);
    }

    // add any convos tht have been modified or created to list of convos
    public void addModifiedConversation(String title) {
      convos.add(title);
    }

    // when a new message is added to given convo, increment number of messgaes
    // in convo
    public void addToMessageCount(String convo) {
      Integer temp = messagesInConvos.get(convo) + 1;
      messagesInConvos.put(convo, temp);
    }

    // when status update is called for convo, reset messaged added since last update
    public void resetMessages(String convo) {
      messagesInConvos.put(convo, 0);
    }

    // returns number of messges that have been added to given convo since last update
    public int getNumOfConvoMessages(String convo) {
      return messagesInConvos.get(convo);
    }

    public String getModifiedConvos() {
      return convos.toString();
    }
  }
