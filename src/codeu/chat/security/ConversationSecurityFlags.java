package codeu.chat.security;

public final class ConversationSecurityFlags {
  public static final int
    VIEW_MESSAGES   = 1 << 0,
    ADD_MESSAGES    = 1 << 1,
    DELETE_MESSAGES = 1 << 2,
    EDIT_MESSAGES   = 1 << 3,
    READ_SECURITY   = 1 << 4,
    MODIFY_SECURITY = 1 << 5,
    FULL_ACCESS     = 0x7FFF_FFFF;
}
