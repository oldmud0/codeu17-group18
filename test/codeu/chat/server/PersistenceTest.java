package codeu.chat.server;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import codeu.chat.common.ConversationHeader;
import codeu.chat.common.RandomUuidGenerator;
import codeu.chat.common.Secret;
import codeu.chat.common.User;
import codeu.chat.server.PersistenceFileSkeleton.ServerInfo;
import codeu.chat.util.Time;
import codeu.chat.util.Uuid;

public final class PersistenceTest {

  private Model model;
  private View view;
  private Controller controller;

  private static final File PERSISTENCE_PATH = new File("storage_test");

  private File persistenceFile;

  private Uuid id;

  private final class MockServerInfo implements ServerInfo {

    @Override
    public Uuid id() {
      return id;
    }

    @Override
    public Secret secret() {
      return new Secret("secret".getBytes());
    }

    @Override
    public Uuid lastSeen() {
      return Uuid.NULL;
    }

  }

  @Before
  public void doBefore() throws IOException {
    model = new Model();
    view = new View(model);
    controller = new Controller(Uuid.NULL, model);
    id = new RandomUuidGenerator(new Uuid(12345), Time.now().inMs()).make();

    if (!PERSISTENCE_PATH.isDirectory())
      PERSISTENCE_PATH.mkdirs();

    persistenceFile = new File(PERSISTENCE_PATH, "persistence.json");

    if (!persistenceFile.exists())
      persistenceFile.createNewFile();
  }

  @Test
  public void testWrite() throws IOException {
    final User user = controller.newUser("Test User");
    final ConversationHeader conversation = controller.newConversation("Test Conversation", user.id);
    controller.newMessage(user.id, conversation.id, "Test body");

    final PersistenceWriter writer = new PersistenceWriter(persistenceFile, view, new MockServerInfo());

    assertFalse("Check that the persistence writer has a valid reference", writer == null);

    writer.write();
  }

}
