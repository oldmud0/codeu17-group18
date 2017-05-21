package codeu.chat.common;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import codeu.chat.util.Uuid;

/**
 * Holds the current version of the CodeU chat server.
 * 
 * For clients, this can be used to determine whether or not the server version
 * matches that of the client. For servers, this is used to advertise the
 * version of the server.
 */
public class VersionInfo {
  private static final String CURRENT_VERSION = "1.0.0";

  private Uuid version;

  /**
   * Creates a new VersionInfo object with the current supported version.
   */
  public VersionInfo() {
    try {
      this.version = Uuid.parse(CURRENT_VERSION);
    } catch (IOException e) {
      this.version = null;
      Logger.getGlobal().log(Level.WARNING, "The current version could not be parsed into a UUID.", e);
    }
  }

  /**
   * Creates a new VersionInfo object with any specific version.
   * 
   * @param version
   *          The UUID representation of the version
   */
  public VersionInfo(Uuid version) {
    this.version = version;
  }

  public Uuid getVersion() {
    return version;
  }
}
