package codeu.chat.client.core;

import java.util.ArrayList;
import java.util.Collection;

import codeu.chat.common.User;
import codeu.chat.client.core.UserContext;
import codeu.chat.util.connections.ConnectionSource;

public class Context extends codeu.chat.contexts.Context {

  public Context(ConnectionSource source) {
    super(new View(source), new Controller(source));
  }

  @Override
  public UserContext create(String name) {
    final User user = controller.newUser(name);
    return user == null ?
        null :
        new UserContext(user, (View) view, (Controller) controller);
  }

  @Override
  public Iterable<codeu.chat.contexts.UserContext> allUsers() {
    final Collection<codeu.chat.contexts.UserContext> users = new ArrayList<>();
    for (final User user : view.getUsers()) {
      users.add(new UserContext(user, (View) view, (Controller) controller));
    }
    return users;
  }

}
