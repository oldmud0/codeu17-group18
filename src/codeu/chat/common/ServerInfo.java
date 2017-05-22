package codeu.chat.common;

import java.util.logging.Level;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import codeu.chat.util.Uuid;

public class ServerInfo {
  private final static String SERVER_VERSION = "1.0.0";

  private Uuid version;

  public ServerInfo() {
    try {
      this.version = Uuid.parse(SERVER_VERSION);
    } catch (IOException e) {
      this.version = null;
      Logger.getGlobal().log(Level.WARNING, "Uuid could not be parsed");
    }
  }

  public ServerInfo(Uuid version) {
    this.version = version;
  }

  public Uuid getVersion() {
    return version;
  }

}
