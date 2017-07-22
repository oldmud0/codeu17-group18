package codeu.chat.server.contexts;

import codeu.chat.common.BasicController;
import codeu.chat.common.BasicView;
import codeu.chat.common.ConversationHeader;
import codeu.chat.common.User;
import codeu.chat.contexts.MessageContext;
import codeu.chat.security.ConversationSecurityFlags;
import codeu.chat.security.SecurityViolationException;
import codeu.chat.util.Uuid;

public class ConversationContext extends codeu.chat.contexts.ConversationContext {

  public ConversationContext(User user, ConversationHeader conversation, BasicView view, BasicController controller) {
    super(user, conversation, view, controller);
  }

  @Override
  public MessageContext add(String messageBody) throws SecurityViolationException {
    if (conversation.security.hasFlags(user.id, ConversationSecurityFlags.ADD_MESSAGES)) {
      return super.add(messageBody);
    }
    throw new SecurityViolationException();
  }

  @Override
  public MessageContext firstMessage() throws SecurityViolationException {
    if (conversation.security.hasFlags(user.id, ConversationSecurityFlags.VIEW_MESSAGES)) {
      return super.firstMessage();
    }
    throw new SecurityViolationException();
  }

  @Override
  public MessageContext lastMessage() throws SecurityViolationException {
    if (conversation.security.hasFlags(user.id, ConversationSecurityFlags.VIEW_MESSAGES)) {
      return super.lastMessage();
    }
    throw new SecurityViolationException();
  }

  @Override
  public void setSecurityFlags(Uuid id, int flags) throws SecurityViolationException {
    if (conversation.security.hasFlags(user.id, ConversationSecurityFlags.MODIFY_SECURITY)) {
      super.setSecurityFlags(id, flags);
    }
    throw new SecurityViolationException();
  }

}
