// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package codeu.chat.contexts;

import java.util.ArrayList;
import java.util.Collection;

import codeu.chat.common.BasicController;
import codeu.chat.common.BasicView;
import codeu.chat.common.ConversationHeader;
import codeu.chat.common.User;
import codeu.chat.security.SecurityViolationException;
import codeu.chat.util.Uuid;

public class UserContext {

  public final User user;
  protected final BasicView view;
  protected final BasicController controller;

  public UserContext(User user, BasicView view, BasicController controller) {
    this.user = user;
    this.view = view;
    this.controller = controller;
  }

  public ConversationContext start(String name) {
    final ConversationHeader conversation = controller.newConversation(name, user.id);
    return conversation == null ?
        null :
        new ConversationContext(user, conversation, view, controller);
  }

  public Iterable<ConversationContext> conversations() {

    // Use all the ids to get all the conversations and convert them to
    // Conversation Contexts.
    final Collection<ConversationContext> all = new ArrayList<>();
    for (final ConversationHeader conversation : view.getConversations()) {
      all.add(new ConversationContext(user, conversation, view, controller));
    }

    return all;
  }

  public String getAllConvosFromServer(Uuid signedInId) {
    return view.getAllConvosFromServer(signedInId);
  }

  public String getNumMessagesFromServer(Uuid signedInId) {
    return view.getNumMessagesFromServer(signedInId);
  }

  public String createUserInterest(String name){
    return controller.newUserInterest(name, user.id);
  }

  public String createConvoInterest(String name){
    return controller.newConvoInterest(name, user.id);
  }

  public String removeUserInterest(String name) {
    return controller.deleteUserInterest(name, user.id);
  }

  public String removeConvoInterest(String name) {
    return controller.deleteConvoInterest(name, user.id);
  }

  public void deleteConversation(Uuid conversationId) throws SecurityViolationException {
    controller.deleteConversation(conversationId);
  }
}
