package codeu.chat.security;

import java.util.HashMap;
import java.util.Map;

import codeu.chat.util.Uuid;

public class ConversationSecurityDescriptor {
  private int implicitSecurity = ConversationSecurityPresets.NONE;
  private Map<Uuid, Integer> explicitSecurity = new HashMap<>();
  
  public ConversationSecurityDescriptor(Uuid owner) {
    explicitSecurity.put(owner, ConversationSecurityPresets.CREATOR);
  }

  public void setPermissions(Uuid invoker, Uuid target, int flags) throws SecurityViolationException {
    if (!hasFlags(invoker, ConversationSecurityFlags.MODIFY_SECURITY)) {
      throw new SecurityViolationException("You do not have access to set permissions.");
    } else if (invoker.equals(target)) {
      throw new SecurityViolationException("You cannot change your own permissions.");
    } else if (getEffectivePermissions(invoker) <= getEffectivePermissions(target)) {
      throw new SecurityViolationException("The user you have to tried to change has equal or higher permissions.");
    } else {
      explicitSecurity.put(target, flags);
    }
  }

  public void resetPermissions(Uuid invoker, Uuid target) throws SecurityViolationException {
    if (!hasFlags(invoker, ConversationSecurityFlags.MODIFY_SECURITY)) {
      throw new SecurityViolationException("You do not have access to set permissions.");
    } else if (invoker.equals(target)) {
      throw new SecurityViolationException("You cannot change your own permissions.");
    } else if (getEffectivePermissions(invoker) <= getEffectivePermissions(target)) {
      throw new SecurityViolationException("The user you have to tried to change has equal or higher permissions.");
    } else {
      explicitSecurity.remove(target);
    }
  }

  public boolean hasFlags(Uuid invoker, int flags) {
    return (getEffectivePermissions(invoker) & flags) == flags;
  }

  private int getEffectivePermissions(Uuid id) {
    if (explicitSecurity.get(id) == null) {
      return implicitSecurity;
    }
    return explicitSecurity.get(id);
  }
}
