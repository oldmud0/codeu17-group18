package codeu.chat.server.contexts;

import codeu.chat.common.BasicController;
import codeu.chat.common.BasicView;
import codeu.chat.common.User;
import codeu.chat.contexts.ConversationContext;
import codeu.chat.util.Uuid;

public class UserContext extends codeu.chat.contexts.UserContext {

  public UserContext(User user, BasicView view, BasicController controller) {
    super(user, view, controller);
  }
  
  @Override
  public ConversationContext start(String name) {
    return super.start(name);
  }

  @Override
  public Iterable<ConversationContext> conversations() {
    return super.conversations();
  }

  @Override
  public String getAllConvosFromServer(Uuid signedInId) {
    return super.getAllConvosFromServer(signedInId);
  }

  @Override
  public String getNumMessagesFromServer(Uuid signedInId) {
    return super.getNumMessagesFromServer(signedInId);
  }

  @Override
  public String createUserInterest(String name) {
    return super.createUserInterest(name);
  }

  @Override
  public String createConvoInterest(String name) {
    return super.createConvoInterest(name);
  }

  @Override
  public String removeUserInterest(String name) {
    return super.removeUserInterest(name);
  }

  @Override
  public String removeConvoInterest(String name) {
    return super.removeConvoInterest(name);
  }

}
