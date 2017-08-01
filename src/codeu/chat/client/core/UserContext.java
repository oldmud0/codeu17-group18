package codeu.chat.client.core;

import codeu.chat.common.User;

public class UserContext extends codeu.chat.contexts.UserContext {

  public UserContext(User user, View view, Controller controller) {
    super(user, new View(view, user), new Controller(controller, user));
  }

}
