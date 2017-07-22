package codeu.chat.client.core;

import codeu.chat.util.connections.ConnectionSource;

public class Context extends codeu.chat.contexts.Context {

  public Context(ConnectionSource source) {
    super(new View(source), new Controller(source));
  }

}
