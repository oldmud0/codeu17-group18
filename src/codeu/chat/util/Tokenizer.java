// Tokenizer
//
// The Tokenizer class changes the input for each command so that it tokenizes
// the input to be a collection of tokens to be a single word or series of words
// surrounded by quotes

package codeu.chat.util;

import java.io.IOException;
import java.lang.StringBuilder;

public final class Tokenizer {

  private final StringBuilder token;
  private final String source;
  private int at;

  public Tokenizer(String source) {
    this.token = new StringBuilder();
    this.source = source;
    this.at = 0;
  }

  // pre: the current index must be less than the length of input
  // post: returns a string representation of the token created from input
  // and reads next word from input
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

  // pre: the current index must be less than the length of input
  // post: returns a string representation of tokens of given input without
  // quotes
  private String readWithNoQuotes() throws IOException{
    token.setLength(0);
    while (remaining() > 0 && !Character.isWhitespace(peek())) {
      token.append(read());
    }
    return token.toString();
  }

  // pre: the current index must be less than the length of input
  // post: returns a string representation of tokens of given input without
  // quotes
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

  // returns the number of letters remaining in input
  private int remaining() {
    return this.source.length() - this.at;
  }

  // pre: the current index must be less than the length of input
  // post: returns letter at current index
  private char peek() throws IOException{
      if (this.at < source.length()) {
        return source.charAt(this.at);
      } else {
        throw new IOException("Index must be less than length");
     }
  }

  // reads and returns the next letter in the input
  private char read() throws IOException{
    final char c = peek();
    this.at = this.at + 1;
    return c;
  }
}
