package codeu.chat.client.core;

import java.util.ArrayList;
import java.util.Collection;

import codeu.chat.common.User;

public class Context extends codeu.chat.contexts.Context {

  private View view;
  private Controller controller;

  public Context(View view, Controller controller) {
    super(view, controller);
    this.view = view;
    this.controller = controller;
  }

  @Override
  public UserContext create(String name) {
    final User user = controller.newUser(name);
    return user == null ?
        null :
        new UserContext(user, view, controller);
  }

  @Override
  public Iterable<codeu.chat.contexts.UserContext> allUsers() {
    final Collection<codeu.chat.contexts.UserContext> users = new ArrayList<>();
    for (final User user : view.getUsers()) {
      users.add(new UserContext(user, view, controller));
    }
    return users;
  }

}
