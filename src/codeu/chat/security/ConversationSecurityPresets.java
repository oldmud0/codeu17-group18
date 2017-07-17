package codeu.chat.security;

import static codeu.chat.security.ConversationSecurityFlags.*;

public final class ConversationSecurityPresets {
  public static final int
    MEMBER  = VIEW_MESSAGES | ADD_MESSAGES,
    OWNER   = MEMBER | DELETE_MESSAGES | EDIT_MESSAGES | READ_SECURITY | MODIFY_SECURITY,
    CREATOR = FULL_ACCESS;
}
