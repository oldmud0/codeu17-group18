package codeu.chat.server.contexts;

import java.util.ArrayList;
import java.util.Collection;

import codeu.chat.common.BasicController;
import codeu.chat.common.BasicView;
import codeu.chat.common.ConversationHeader;
import codeu.chat.common.User;
import codeu.chat.contexts.ConversationContext;
import codeu.chat.security.ConversationSecurityFlags;
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
    final Collection<ConversationContext> all = new ArrayList<>();
    for (final ConversationHeader conversation : view.getConversations()) {
      if (conversation.security.hasFlags(user.id, ConversationSecurityFlags.VIEW_MESSAGES)) {
        all.add(new ConversationContext(user, conversation, view, controller));
      }
    }
    return all;
  }

  @Override
  public String getAllConvosFromServer(Uuid signedInId) {
    // TODO add security checks
    return super.getAllConvosFromServer(signedInId);
  }

  @Override
  public String getNumMessagesFromServer(Uuid signedInId) {
    // TODO add security checks
    return super.getNumMessagesFromServer(signedInId);
  }

  @Override
  public String createUserInterest(String name) {
    // TODO add security checks
    return super.createUserInterest(name);
  }

  @Override
  public String createConvoInterest(String name) {
    // TODO add security checks
    return super.createConvoInterest(name);
  }

  @Override
  public String removeUserInterest(String name) {
    // TODO add security checks
    return super.removeUserInterest(name);
  }

  @Override
  public String removeConvoInterest(String name) {
    // TODO add security checks
    return super.removeConvoInterest(name);
  }

}
