package codeu.chat.server.contexts;

import codeu.chat.common.BasicController;
import codeu.chat.common.BasicView;
import codeu.chat.common.ConversationHeader;
import codeu.chat.common.User;
import codeu.chat.contexts.MessageContext;

public class ConversationContext extends codeu.chat.contexts.ConversationContext {

  public ConversationContext(User user, ConversationHeader conversation, BasicView view, BasicController controller) {
    super(user, conversation, view, controller);
  }

  @Override
  public MessageContext add(String messageBody) {
    return super.add(messageBody);
  }

  @Override
  public MessageContext firstMessage() {
    return super.firstMessage();
  }

  @Override
  public MessageContext lastMessage() {
    return super.lastMessage();
  }

}
