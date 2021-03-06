// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package codeu.chat.client.commandline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import codeu.chat.client.core.UserContext;
import codeu.chat.common.User;
import codeu.chat.common.VersionInfo;
import codeu.chat.contexts.Context;
import codeu.chat.contexts.ConversationContext;
import codeu.chat.contexts.MessageContext;
import codeu.chat.security.ConversationSecurityPresets;
import codeu.chat.security.SecurityViolationException;
import codeu.chat.util.ServerInfo;
import codeu.chat.util.Tokenizer;
import codeu.chat.util.Uuid;

public final class Chat {

  // PANELS
  //
  // We are going to use a stack of panels to track where in the application
  // we are. The command will always be routed to the panel at the top of the
  // stack. When a command wants to go to another panel, it will add a new
  // panel to the top of the stack. When a command wants to go to the previous
  // panel all it needs to do is pop the top panel.
  private final Stack<Panel> panels = new Stack<>();
  private Context context2;

  public Chat(Context context) {
    this.panels.push(createRootPanel(context));
  }

  // HANDLE COMMAND
  //
  // Take a single line of input and parse a command from it. If the system
  // is willing to take another command, the function will return true. If
  // the system wants to exit, the function will return false.
  //
  public boolean handleCommand(String line) throws IOException {

    final List<String> args = new ArrayList<String>();
    Tokenizer tokenizer = new Tokenizer(line);
    for (String token = tokenizer.next(); token != null; token = tokenizer.next()) {
      args.add(token);
    }
    if (args.isEmpty())
      return true;
    final String command = args.remove(0);

    // Because "exit" and "back" are applicable to every panel, handle
    // those commands here to avoid having to implement them for each
    // panel.

    if ("exit".equals(command)) {
      // The user does not want to process any more commands
      return false;
    }

    // Do not allow the root panel to be removed.
    if ("back".equals(command) && panels.size() > 1) {
      panels.pop();
      return true;
    }

    if (panels.peek().handleCommand(command, args)) {
      // the command was handled
      return true;
    }

    // If we get to here it means that the command was not correctly handled
    // so we should let the user know. Still return true as we want to
    // continue
    // processing future commands.
    System.out.println("ERROR: Unsupported command");
    return true;
  }

  // CREATE ROOT PANEL
  //
  // Create a panel for the root of the application. Root in this context
  // means
  // the first panel and the only panel that should always be at the bottom of
  // the panels stack.
  //
  // The root panel is for commands that require no specific contextual
  // information.
  // This is before a user has signed in. Most commands handled by the root
  // panel
  // will be user selection focused.
  //
  private Panel createRootPanel(final Context context) {

    final Panel panel = new Panel();

    // HELP
    //
    // Add a command to print a list of all commands and their description
    // when
    // the user for "help" while on the root panel.
    //
    panel.register("help", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        System.out.println("ROOT MODE");
        System.out.println("  u-list");
        System.out.println("    List all users.");
        System.out.println("  u-add <name>");
        System.out.println("    Add a new user with the given name.");
        System.out.println("  u-sign-in <name>");
        System.out.println("    Sign in as the user with the given name.");
        System.out.println("  version");
        System.out.println("    Print the server version.");
        System.out.println("  exit");
        System.out.println("    Exit the program.");
      }
    });

    // U-LIST (user list)
    //
    // Add a command to print all users registered on the server when the
    // user
    // enters "u-list" while on the root panel.
    //
    panel.register("u-list", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        for (final codeu.chat.contexts.UserContext user : context.allUsers()) {
          System.out.format("USER %s (UUID:%s)\n", user.user.name, user.user.id);
        }
      }
    });

    // U-ADD (add user)
    //
    // Add a command to add and sign-in as a new user when the user enters
    // "u-add" while on the root panel.
    //
    panel.register("u-add", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        final String name = args.remove(0);
        if (name.length() > 0) {
          if (context.create(name) == null) {
            System.out.println("ERROR: Failed to create new user");
          }
        } else {
          System.out.println("ERROR: Missing <username>");
        }
      }
    });

    // U-SIGN-IN (sign in user)
    //
    // Add a command to sign-in as a user when the user enters "u-sign-in"
    // while on the root panel.
    //
    panel.register("u-sign-in", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        final String name = args.remove(0);
        if (name.length() > 0) {
          final UserContext user = findUser(name);
          if (user == null) {
            System.out.format("ERROR: Failed to sign in as '%s'\n", name);
          } else {
            panels.push(createUserPanel(user));
          }
        } else {
          System.out.println("ERROR: Missing <username>");
        }
      }

      // Find the first user with the given name and return a user context
      // for that user. If no user is found, the function will return
      // null.
      private UserContext findUser(String name) {
        for (final codeu.chat.contexts.UserContext user : context.allUsers()) {
          if (user.user.name.equals(name)) {
            return (UserContext) user;
          }
        }
        return null;
      }
    });

    // VERSION (server version)
    //
    // Print the server's version.
    //
    panel.register("version", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        final VersionInfo version = context.getVersion();
        if (version == null) {
          System.out.println("ERROR: No version returned");
        } else {
          System.out.format("Server version: %s\n", version.toString());
        }
      }
    });

    panel.register("serverinfo", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        final ServerInfo info = context.getInfo();
        if (info == null) {
          System.out.println("ERROR: No info returned");
        } else {
          System.out.format("Server info: %s\n", info.toString());
        }
      }
    });

    // Now that the panel has all its commands registered, return the panel
    // so that it can be used.
    context2 = context;
    return panel;
  }

  private Panel createUserPanel(final UserContext user) {

    final Panel panel = new Panel();

    // HELP
    //
    // Add a command that will print a list of all commands and their
    // descriptions when the user enters "help" while on the user panel.
    //
    panel.register("help", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        System.out.println("USER MODE");
        System.out.println("  c-list");
        System.out.println("    List all conversations that the current user can interact with.");
        System.out.println("  c-add <title>");
        System.out.println(
            "    Add a new conversation with the given title and join it as the current user.");
        System.out.println("  c-delete <id>");
        System.out.println("    Delete a conversation.");
        System.out.println("  c-add-userInterest <name>");
        System.out.println("    Add this user with the given name to user's interest system.");
        System.out.println("  c-add-convoInterest <title>");
        System.out
            .println("    Add this conversation with the given title to user's interest system.");
        System.out.println("  c-remove-userInterest <name>");
        System.out.println("    Remove this user with the given name from user's interest system.");
        System.out.println("  c-remove-convoInterest <title>");
        System.out.println(
            "    Remove this conversation with the given title from user's interest system.");
        System.out.println("  c-join <title>");
        System.out.println("    Join the conversation as the current user.");
        System.out.println("  c-convo-statusUpdate");
        System.out.println(
            "      Prints how many messages have been added to interested conversations since last update.");
        System.out.println("  c-user-statusUpdate");
        System.out.println(
            "      Prints all conversations that have been created or modified for each interested user.");
        System.out.println("  info");
        System.out.println("    Display all info for the current user");
        System.out.println("  back");
        System.out.println("    Go back to ROOT MODE.");
        System.out.println("  exit");
        System.out.println("    Exit the program.");
      }
    });

    // C-LIST (list conversations)
    //
    // Add a command that will print all conversations when the user enters
    // "c-list" while on the user panel.
    //
    panel.register("c-list", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        for (final ConversationContext conversation : user.conversations()) {
          System.out.format("CONVERSATION %s (UUID:%s)\n", conversation.conversation.title,
              conversation.conversation.id);
        }
      }
    });

    panel.register("c-add-userInterest", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        final String name = args.remove(0);

        if (name.length() > 0) {
          if (user.createUserInterest(name) == null) {
            System.out.println("ERROR: Failed to create new userInterest");
          } else {
            System.out.println(user.createUserInterest(name));
          }
        }
      }
    });

    panel.register("c-add-convoInterest", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        final String name = args.remove(0);

        if (name.length() > 0) {
          if (user.createConvoInterest(name) == null) {
            System.out.println("ERROR: Failed to create new convoInterest");
          } else {
            System.out.println(user.createConvoInterest(name));
          }
        }
      }
    });

    panel.register("c-remove-userInterest", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        final String name = args.remove(0);
        if (name.length() > 0) {
          if (user.removeUserInterest(name) == null) {
            System.out.println("ERROR: Failed to remove userInterest");
          } else {
            System.out.println(user.removeUserInterest(name));
          }
        }
      }
    });

    panel.register("c-remove-convoInterest", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        final String name = args.remove(0);
        if (name.length() > 0) {
          if (user.removeConvoInterest(name) == null) {
            System.out.println("ERROR: Failed to remove userInterest");
          } else {
            System.out.println(user.removeConvoInterest(name));
          }
        }
      }
    });

    // C-ADD (add conversation)
    //
    // Add a command that will create and join a new conversation when the
    // user
    // enters "c-add" while on the user panel.
    //
    panel.register("c-add", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        final String name = args.remove(0);
        if (name.length() > 0) {
          final ConversationContext conversation = user.start(name);
          if (conversation == null) {
            System.out.println("ERROR: Failed to create new conversation");
          } else {
            panels.push(createConversationPanel(conversation));
          }
        } else {
          System.out.println("ERROR: Missing <title>");
        }
      }
    });

    // C-DELETE (delete conversation)
    //
    // Add a command that will delete a conversation when the user
    // enters "c-delete" on the user panel and specifies the UUID of the
    // conversation as a parameter.
    panel.register("c-delete", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        try {
          final Uuid conversationId = Uuid.parse(args.remove(0));
          ConversationContext targetConversation = null;
          if (conversationId != null) {
            // Find the conversation
            for (ConversationContext convo : user.conversations()) {
              if (convo.conversation.id.equals(conversationId)) {
                targetConversation = convo;
              }
            }
            if (targetConversation != null) {
              user.deleteConversation(targetConversation.conversation.id);
            } else {
              System.out.println("ERROR: Conversation of specified ID was not found.");
            }
          } else {
            System.out.println("ERROR: Conversation ID cannot be null.");
          }
        } catch (SecurityViolationException e) {
          System.out.println("You are not allowed to delete this conversation.");
        } catch (IOException | IndexOutOfBoundsException e) {
          System.out.println("Conversation ID could not be parsed.");
        }
      }
    });

    // C-JOIN (join conversation)

    // Add a command that will joing a conversation when the user enters
    // "c-join" while on the user panel.
    panel.register("c-join", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        final String name = args.remove(0);
        if (name.length() > 0) {
          final ConversationContext conversation = find(name);
          if (conversation == null) {
            System.out.format("ERROR: No conversation with name '%s'\n", name);
          } else {
            panels.push(createConversationPanel(conversation));
          }
        } else {
          System.out.println("ERROR: Missing <title>");
        }
      }

      // Find the first conversation with the given name and return its
      // context.
      // If no conversation has the given name, this will return null.
      private ConversationContext find(String title) {
        for (final ConversationContext conversation : user.conversations()) {
          if (title.equals(conversation.conversation.title)) {
            return conversation;
          }
        }
        return null;
      }
    });

    panel.register("c-convo-statusUpdate", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        final Uuid signedInId = user.user.id;
        final String convoStatusUpdate = user.getNumMessagesFromServer(signedInId);

        if (convoStatusUpdate == null) {
          System.out.println("ERROR: No user status update returned");
        } else {
          System.out.print(convoStatusUpdate);
        }
      }

    });

    panel.register("c-user-statusUpdate", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        final Uuid signedInId = user.user.id;
        final String userStatusUpdate = user.getAllConvosFromServer(signedInId);
        if (userStatusUpdate == null) {
          System.out.println("ERROR: No user status update returned");
        } else {
          System.out.print(userStatusUpdate);
        }
      }
    });

    // INFO
    //
    // Add a command that will print info about the current context when the
    // user enters "info" while on the user panel.
    //
    panel.register("info", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        System.out.println("User Info:");
        System.out.format("  Name : %s\n", user.user.name);
        System.out.format("  Id   : UUID:%s\n", user.user.id);
      }
    });

    // Now that the panel has all its commands registered, return the panel
    // so that it can be used.
    return panel;
  }

  private Panel createConversationPanel(final ConversationContext conversation) {

    final Panel panel = new Panel();

    // HELP
    //
    // Add a command that will print all the commands and their descriptions
    // when the user enters "help" while on the conversation panel.
    //
    panel.register("help", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        System.out.println("USER MODE");
        System.out.println("  m-list");
        System.out.println("    List all messages in the current conversation.");
        System.out.println("  m-add <message>");
        System.out.println("  m-delete <id>");
        System.out.println("    Delete a message from the current conversation.");
        System.out
            .println("    Add a new message to the current conversation as the current user.");
        System.out.println("  c-set-access <name> <none|member|owner>");
        System.out.println(
            "    Set the access level of the current conversation to the specified preset for a user.");
        System.out.println("  info");
        System.out.println("    Display all info about the current conversation.");
        System.out.println("  back");
        System.out.println("    Go back to USER MODE.");
        System.out.println("  exit");
        System.out.println("    Exit the program.");
      }
    });

    // M-LIST (list messages)
    //
    // Add a command to print all messages in the current conversation when
    // the
    // user enters "m-list" while on the conversation panel.
    //
    panel.register("m-list", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        try {
          System.out.println("--- start of conversation ---");
          for (MessageContext message = conversation.firstMessage(); message != null; message =
              message.next()) {
            System.out.println();
            System.out.format("USER : %s\n", message.message.author);
            System.out.format("SENT : %s\n", message.message.creation);
            System.out.format("ID : %s\n", message.message.id);
            System.out.println();
            System.out.println(message.message.content);
            System.out.println();
          }
          System.out.println("---  end of conversation  ---");
        } catch (SecurityViolationException e) {
          System.out.println("You are not allowed to view this conversation.");
        }
      }
    });

    // M-ADD (add message)
    //
    // Add a command to add a new message to the current conversation when
    // the
    // user enters "m-add" while on the conversation panel.
    //
    panel.register("m-add", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        try {
          final String message = args.remove(0);
          if (message.length() > 0) {
            conversation.add(message);
          } else {
            System.out.println("ERROR: Messages must contain text");
          }
        } catch (SecurityViolationException e) {
          System.out.println("You are not allowed to add messages to this conversation.");
        }
      }
    });

    // M-DELETE
    //
    // Add a command to delete a message in the current conversation by
    // taking in the Uuid of a message.
    //
    panel.register("m-delete", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        try {
          final Uuid messageID = Uuid.parse(args.remove(0));
          User currentUser = conversation.user;
          MessageContext targetMessage = null;
          if (messageID != null) {
            for (MessageContext message = conversation.firstMessage(); message != null; message =
                message.next()) {
              if (message.message.id.equals(messageID)) {
                targetMessage = message;
              }
            }
            if (targetMessage != null) {
              conversation.remove(targetMessage.message.id);
            } else {
              System.out.println("ERROR: No message of ID found in conversation.");
            }
          } else {
            System.out.println("ERROR: Message ID cannot be null.");
          }
        } catch (SecurityViolationException e) {
          System.out.println("You are not allowed to delete messages in this conversation.");
        } catch (IOException | IndexOutOfBoundsException e) {
          System.out.println("Message ID could not be parsed.");
        }
      }
    });
    // C-SET-ACCESS (set access for user)
    //
    // Add a command that will set access for another user in a conversation
    // if the logged in user has the correct flags
    // User must type "c-set-access <id> <flag>" while on the user panel.
    //
    panel.register("c-set-access", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        try {
          final String userSearched = args.remove(0);
          final String preset = (args.remove(0)).toLowerCase();
          User targetUser = null;

          for (final codeu.chat.contexts.UserContext user : context2.allUsers()) {
            if (user.user.name.equalsIgnoreCase(userSearched)) {
              targetUser = user.user;
            }
          }

          int flags;
          if (preset.equals("owner")) {
            flags = ConversationSecurityPresets.OWNER;
          } else if (preset.equals("member")) {
            flags = ConversationSecurityPresets.MEMBER;
          } else if (preset.equals("none")) {
            flags = ConversationSecurityPresets.NONE;
          } else {
            System.out.println("ERROR: Please enter a valid security preset.");
            return;
          }
          if (targetUser != null) {
            conversation.setSecurityFlags(targetUser.id, flags);
          } else {
            System.out.println("ERROR: Missing <name>");
          }
        } catch (IndexOutOfBoundsException e) {
          System.out.println("ERROR: Unable to find an argument.");
        } catch (SecurityViolationException e) {
          System.out.println("You are not allowed to change the permissions of this conversation.");
        }
      }
    });

    // INFO
    //
    // Add a command to print info about the current conversation when the
    // user
    // enters "info" while on the conversation panel.
    //
    panel.register("info", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        System.out.println("Conversation Info:");
        System.out.format("  Title : %s\n", conversation.conversation.title);
        System.out.format("  Id    : UUID:%s\n", conversation.conversation.id);
        System.out.format("  Owner : %s\n", conversation.conversation.owner);
      }
    });

    // Now that the panel has all its commands registered, return the panel
    // so that it can be used.
    return panel;
  }
}
