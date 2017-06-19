package codeu.chat.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import codeu.chat.common.ConversationHeader;
import codeu.chat.common.RandomUuidGenerator;
import codeu.chat.common.Secret;
import codeu.chat.common.User;
import codeu.chat.common.VersionInfo;
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

  private final ServerInfo serverInfo = new ServerInfo() {

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

    @Override
    public VersionInfo version() {
      return new VersionInfo();
    }

  };

  @Before
  public void doBefore() throws IOException {
    model = new Model();
    view = new View(model);
    id = new RandomUuidGenerator(new Uuid(12345), Time.now().inMs()).make();
    controller = new Controller(id, model);

    if (!PERSISTENCE_PATH.isDirectory())
      PERSISTENCE_PATH.mkdirs();

    persistenceFile = new File(PERSISTENCE_PATH, "persistence.json");

    if (!persistenceFile.exists())
      persistenceFile.createNewFile();
  }

  private void createData() {
    final User user = controller.newUser("Test User");
    final ConversationHeader conversation = controller.newConversation("Test Conversation", user.id);
    controller.newMessage(user.id, conversation.id, "Test body");
  }

  @Test
  public void testWrite() throws IOException {
    createData();

    final PersistenceWriter writer = new PersistenceWriter(persistenceFile, view, serverInfo);

    assertFalse("Check that the persistence writer has a valid reference", writer == null);

    writer.write();
  }

  @Test
  public void testRead() throws IOException {
    testWrite();

    final PersistenceReader reader = new PersistenceReader(persistenceFile);

    assertFalse("Check that the persistence reader has a valid reference", reader == null);

    reader.read();

    assertFalse("Check that the persistence reader actually created the container", reader.getContainer() == null);
  }

  @Test
  public void testRewrite() throws IOException {
    // Write initial data
    testWrite();

    final PersistenceReader reader = new PersistenceReader(persistenceFile);

    // Read initial data
    reader.read();

    assertFalse("Check that the persistence reader actually created the first container",
        reader.getContainer() == null);

    // Put initial data into container 1
    PersistenceFileSkeleton container1 = reader.getContainer();

    final PersistenceWriter writer = new PersistenceWriter(persistenceFile, container1);

    assertFalse("Check that the persistence writer has a valid reference", writer == null);

    // Write the data we just read
    writer.write();

    // Read that data again
    reader.read();

    assertFalse("Check that the persistence reader actually created the second container",
        reader.getContainer() == null);

    // Put reread data into container 2
    PersistenceFileSkeleton container2 = reader.getContainer();

    // Reread file should be the same as the original read file.
    assertEquals(container1.conversationHeaders(), container2.conversationHeaders());
    assertEquals(container1.conversationPayloads(), container2.conversationPayloads());
    assertEquals(container1.users(), container2.users());
    assertEquals(container1.messages(), container2.messages());
    assertEquals(container1.serverInfo(), container2.serverInfo());
  }
}
