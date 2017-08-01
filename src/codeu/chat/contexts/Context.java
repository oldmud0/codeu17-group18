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
import codeu.chat.common.User;
import codeu.chat.common.VersionInfo;
import codeu.chat.util.ServerInfo;

public abstract class Context {

  private BasicView view;
  private BasicController controller;
  
  public Context(BasicView view, BasicController controller) {
    this.view = view;
    this.controller = controller;
  }

  public UserContext create(String name) {
    final User user = controller.newUser(name);
    return user == null ?
        null :
        new UserContext(user, view, controller);
  }

  public Iterable<UserContext> allUsers() {
    final Collection<UserContext> users = new ArrayList<>();
    for (final User user : view.getUsers()) {
      users.add(new UserContext(user, view, controller));
    }
    return users;
  }
  
  public VersionInfo getVersion() {
    return view.getVersion();
  }

  public ServerInfo getInfo() {
    return view.getInfo();
  }

}
