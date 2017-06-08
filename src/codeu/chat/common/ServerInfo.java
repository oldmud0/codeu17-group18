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
    this(getCurrentUuid());
  }

  public ServerInfo(Uuid version) {
    this.version = version;
  }

  public Uuid getVersion() {
    return version;
  }
  
  private static Uuid getCurrentUuid(){
    try{
      return Uuid.parse(SERVER_VERSION);
    }catch(IOException e){
      Logger.getGlobal().log(Level.WARNING, "Uuid could not be parsed");
      return null;
    }
  }
}
