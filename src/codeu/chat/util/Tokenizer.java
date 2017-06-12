package codeu.chat.util;

import java.io.*;
import java.io.IOException;
import java.lang.*;

public final class Tokenizer {

  private StringBuilder token;
  private String source;
  private int at;

  public Tokenizer(String source) {
    this.token = new StringBuilder();
    this.source = source;
    this.at = 0;
  }

  public String next() throws IOException {
    while (remaining() > 0 && Character.isWhitespace(peek())) {
      read();
    }
    if (remaining() <= 0) {
      return null;
    } else if (peek() == '"') {
      return readWithQuotes();
    } else {
      return readWithNoQuotes();
    }
  }

  private String readWithNoQuotes() throws IOException{
    token.setLength(0);
    while (remaining() > 0 && !Character.isWhitespace(peek())) {
      token.append(read());
    }
    return token.toString();
  }


  private String readWithQuotes() throws IOException {
    token.setLength(0);
    if (read() != '"') {
      throw new IOException("Strings must start with opening quote");
    }
    while (peek() != '"') {
      token.append(read());
    }
    read();
    return token.toString();
  }

  private int remaining() {
    return this.source.length() - this.at;
  }

  private char peek() throws IOException{
      if (this.at < source.length()) {
        return source.charAt(this.at);
      } else {
        throw new IOException("Index must be less than length");
     }
  }

  private char read() throws IOException{
    final char c = peek();
    this.at = this.at + 1;
    return c;
  }
}
