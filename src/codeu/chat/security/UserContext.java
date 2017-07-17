package codeu.chat.security;

import codeu.chat.common.User;
import codeu.chat.server.Controller;
import codeu.chat.server.View;

/**
 * A security context which validates user actions against permissions before
 * they are performed.
 * 
 * All requests coming from users should pass through a UserContext instead of
 * being routed directly to a View or Controller, to prevent any security
 * problems.
 * 
 * TODO: do we want to derive the security contexts from the existing
 * {@link codeu.chat.client.core.UserContext UserContext} et al. classes, or
 * make these unique?
 */
public class UserContext {

  private final User user;
  private final View view;
  private final Controller controller;

  public UserContext(User user, View view, Controller controller) {
    this.user = user;
    this.view = view;
    this.controller = controller;
  }

}
