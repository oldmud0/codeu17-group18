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

package codeu.chat.client.core;

import codeu.chat.common.BasicController;
import codeu.chat.common.ConversationHeader;
import codeu.chat.common.Message;
import codeu.chat.common.NetworkCode;
import codeu.chat.common.User;
import codeu.chat.security.SecurityViolationException;
import codeu.chat.util.Logger;
import codeu.chat.util.Serializers;
import codeu.chat.util.Uuid;
import codeu.chat.util.connections.Connection;
import codeu.chat.util.connections.ConnectionSource;

public final class Controller implements BasicController {

  private final static Logger.Log LOG = Logger.newLog(Controller.class);

  private final ConnectionSource source;
  private User user;

  public Controller(ConnectionSource source) {
    this.source = source;
  }

  public Controller(Controller controller, User user) {
    this(controller.source);
    this.user = user;
  }

  @Override
  public Message newMessage(Uuid author, Uuid conversation, String body) {

    Message response = null;

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.NEW_MESSAGE_REQUEST);
      Uuid.SERIALIZER.write(connection.out(), author);
      Uuid.SERIALIZER.write(connection.out(), conversation);
      Serializers.STRING.write(connection.out(), body);

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.NEW_MESSAGE_RESPONSE) {
        response = Serializers.nullable(Message.SERIALIZER).read(connection.in());
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }

    return response;
  }

  @Override
  public User newUser(String name) {

    User response = null;

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.NEW_USER_REQUEST);
      Serializers.STRING.write(connection.out(), name);
      LOG.info("newUser: Request completed.");

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.NEW_USER_RESPONSE) {
        response = Serializers.nullable(User.SERIALIZER).read(connection.in());
        LOG.info("newUser: Response completed.");
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }

    return response;
  }

  @Override
  public String newUserInterest(String name, Uuid signedInId) {

    String response = null;

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.NEW_USER_INTEREST_REQUEST);
      Serializers.STRING.write(connection.out(), name);
      Uuid.SERIALIZER.write(connection.out(), signedInId);
      LOG.info("newInterestUser: Request completed.");

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.NEW_USER_INTEREST_RESPONSE) {
        response = new String(Serializers.STRING.read(connection.in()));
        LOG.info("newInterestUser: Response completed.");
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }
    return response;
  }

  @Override
  public String newConvoInterest(String title, Uuid owner) {
    String response = null;

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.NEW_CONVO_INTEREST_REQUEST);
      Serializers.STRING.write(connection.out(), title);
      Uuid.SERIALIZER.write(connection.out(), owner);
      LOG.info("newInterestConvo: Request completed.");

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.NEW_CONVO_INTEREST_RESPONSE) {
        response = new String(Serializers.STRING.read(connection.in()));
        LOG.info("newInterestConvo: Response completed.");
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }
    return response;
  }

  @Override
  public String deleteUserInterest(String name, Uuid signedInId) {

    String response = null;

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.REMOVE_USER_INTEREST_REQUEST);
      Serializers.STRING.write(connection.out(), name);
      Uuid.SERIALIZER.write(connection.out(), signedInId);
      LOG.info("removeInterestUser: Request completed.");

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.REMOVE_USER_INTEREST_RESPONSE) {
        response = new String(Serializers.STRING.read(connection.in()));
        LOG.info("removeInterestUser: Response completed.");
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }
    return response;
  }

  @Override
  public String deleteConvoInterest(String name, Uuid signedInId) {
    String response = null;

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.REMOVE_CONVO_INTEREST_REQUEST);
      Serializers.STRING.write(connection.out(), name);
      Uuid.SERIALIZER.write(connection.out(), signedInId);
      LOG.info("removeInterestConvo: Request completed.");

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.REMOVE_CONVO_INTEREST_RESPONSE) {
        response = new String(Serializers.STRING.read(connection.in()));
        LOG.info("removeInterestConvo: Response completed.");
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }
    return response;
  }

  @Override
  public ConversationHeader newConversation(String title, Uuid owner)  {

    ConversationHeader response = null;

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.NEW_CONVERSATION_REQUEST);
      Serializers.STRING.write(connection.out(), title);
      Uuid.SERIALIZER.write(connection.out(), owner);

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.NEW_CONVERSATION_RESPONSE) {
        response = Serializers.nullable(ConversationHeader.SERIALIZER).read(connection.in());
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }

    return response;
  }

  @Override
  public void setConversationExplicitPermissions(Uuid convoID, Uuid invoker, Uuid target, int flags) throws SecurityViolationException {

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.NEW_ACCESS_CONTROL_REQUEST);
      Uuid.SERIALIZER.write(connection.out(), convoID);
      Uuid.SERIALIZER.write(connection.out(), invoker);
      Uuid.SERIALIZER.write(connection.out(), target);
      Serializers.INTEGER.write(connection.out(), flags);

      LOG.info("SetConversationExplicitPermissions: Request completed.");
      int returnCode = Serializers.INTEGER.read(connection.in());
      if (returnCode == NetworkCode.NEW_ACCESS_CONTROL_RESPONSE) {
        LOG.info("SetConversationExplicitPermissions: Response completed.");
      } else if (returnCode == NetworkCode.ERR_SECURITY_VIOLATION) {
        throw new SecurityViolationException();
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }
  }
  
  @Override
  public void deleteMessage(Uuid convoId, Uuid messageId) throws SecurityViolationException {
	  try (final Connection connection = source.connect()) {
		  Serializers.INTEGER.write(connection.out(), NetworkCode.DELETE_MESSAGE_REQUEST);
		  Uuid.SERIALIZER.write(connection.out(), convoId);
	      Uuid.SERIALIZER.write(connection.out(), messageId);
	      Uuid.SERIALIZER.write(connection.out(), user.id);
	      
	      LOG.info("DeleteMessage: Request completed.");
	      int returnCode = Serializers.INTEGER.read(connection.in());
	      if (returnCode == NetworkCode.DELETE_MESSAGE_RESPONSE) {
	        LOG.info("DeleteMessage: Response completed.");
	      } else if (returnCode == NetworkCode.ERR_SECURITY_VIOLATION) {
	        throw new SecurityViolationException();
	      } else {
	        LOG.error("Response from server failed.");
	      }
	  } catch (SecurityViolationException e) {
	    throw e;
	  } catch (Exception ex) {
		  System.out.println("ERROR: Exception during call on server. Check log for details.");
	      LOG.error(ex, "Exception during call on server.");
	  }
  }

  @Override
  public void deleteConversation(Uuid conversationId) throws SecurityViolationException {
    try (final Connection connection = source.connect()) {
      Serializers.INTEGER.write(connection.out(), NetworkCode.DELETE_CONVERSATION_REQUEST);
      Uuid.SERIALIZER.write(connection.out(), conversationId);
      Uuid.SERIALIZER.write(connection.out(), user.id);

      LOG.info("DeleteConversation: Request completed.");
      int returnCode = Serializers.INTEGER.read(connection.in());
      if (returnCode == NetworkCode.DELETE_CONVERSATION_RESPONSE) {
        LOG.info("DeleteConversation: Response completed.");
      } else if (returnCode == NetworkCode.ERR_SECURITY_VIOLATION) {
        throw new SecurityViolationException();
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (SecurityViolationException e) {
      throw e;
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }
  }
}
