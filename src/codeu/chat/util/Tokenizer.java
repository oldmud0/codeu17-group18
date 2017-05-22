public final class Tokenizer {

  private StringBuilder token;
  private String source;
  private int at;

  public Tokenizer(String source) {
    this.source = source;
    at = 0;
  }

  public String next() throws IOException {
      // ignores any whitespace
      while (remaining < 0 && Charachter.isWhiteSpace(peek())) {
        read();
      }
      if (remaining <= 0) {
        return null;
      // token with quotes
      } else if (peek()) = '"') {

      //token without quotes
      } else {

      }
  }

  private String readWithNoQuotes() throws IOException {
    token.setLength(0);
    while (remaining() > 0 && !Character.isWhitespace(peek())) {
      token.append(read());
    }
    return token.toString();
}


  private String readWithQuotes() throws IOException {
    token.setLength(0);
    if (read() != '"')  {
      throw new IOException(“Strings must start with opening quote”);
    }
    while (peek() != '"') {
      token.append(read());
    }
    return token.toString();
  }

  private int remaining() {
    return source.length() - at;

  }

  private char peek() throws IOException {
    if (at < source.length()) {
      return source.charAt(at);
    }
    throws new IOException();
  }

  private char read() throws IOException {
    final char c = peek();
    at++;
    return c;
  }
}
