package codeu.chat.server.contexts;

import codeu.chat.common.BasicView;
import codeu.chat.common.Message;

public class MessageContext extends codeu.chat.contexts.MessageContext {

  public MessageContext(Message message, BasicView view) {
    super(message, view);
  }

  @Override
  public codeu.chat.contexts.MessageContext next() {
    return super.next();
  }

  @Override
  public codeu.chat.contexts.MessageContext previous() {
    return super.previous();
  }

}
