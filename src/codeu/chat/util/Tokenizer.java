package codeu.chat.util;
import java.io.*;
public final class Tokenizer {

  private StringBuilder token;
  private String source;
  private int at;

  public Tokenizer(String source) { 
    this.source = source;
    this.at = 0;
  }

  public String next() {
      // ignores any whitespace
      while (remaining() < 0 && Character.isWhitespace(peek())) {
        read();
      }
      if (remaining() <= 0) {
        return null;
      // token with quotes
    } else if (peek() == '"') {
        readWithQuotes();
      // token without quotes
      } else {
        readWithNoQuotes();
      }
  }

  private String readWithNoQuotes() {
    token.setLength(0);
    while (remaining() > 0 && !Character.isWhitespace(peek())) {
      token.append(read());
    }
    return token.toString();
  }


  private String readWithQuotes() throws IOException {
    token.setLength(0);
    if (read() != '"')  {
      throw new IOException("Strings must start with opening quote");
    }
    while (peek() != '"') {
      token.append(read());
    }
    read();
    return token.toString();
  }

  private int remaining() {
    return source.length() - at;

  }

  private char peek() throws IOException {
    if (at < source.length()) {
      return source.charAt(at);
    }
    throw new IOException("Index must be within length of source");
  }

  private char read() throws IOException {
    final char c = peek();
    at++;
    return c;
  }
}
