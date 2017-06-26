package codeu.chat.server;

import codeu.chat.util.Logger;
import codeu.chat.util.Timeline;

/**
 * Intended to be run on the server timeline as a separate thread, this class
 * calls a {@link PersistenceWriter} to write the persistence file at a regular
 * interval.
 * 
 * @see codeu.chat.util.Timeline
 */
public class PersistenceWriterRunnable implements Runnable {

  /**
   * The interval, in milliseconds, which the persistence file will be written
   * to.
   */
  public static final int WRITE_INTERVAL_MS = 30000;

  private static final Logger.Log LOG = Logger.newLog(PersistenceWriterRunnable.class);

  private final PersistenceWriter writer;

  private final Timeline timeline;

  public PersistenceWriterRunnable(PersistenceWriter writer, Timeline timeline) {
    this.writer = writer;
    this.timeline = timeline;
  }

  /** Calls the PersistenceWriter and reschedules itself. **/
  public void run() {
    try {
      writer.write();
      LOG.verbose("Successfully written to persistence file.");
    } catch (Exception ex) {
      LOG.error(ex, "Exception thrown while writing to persistence file. Data loss may have occurred.");
    }
    timeline.scheduleIn(WRITE_INTERVAL_MS, this);
  }
}
