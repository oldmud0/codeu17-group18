package codeu.chat.security;

public class SecurityViolationException extends Exception {

  private static final long serialVersionUID = -5146785080734996313L;

  public SecurityViolationException() {
  }

  public SecurityViolationException(String message) {
    super(message);
  }

  public SecurityViolationException(Throwable cause) {
    super(cause);
  }

  public SecurityViolationException(String message, Throwable cause) {
    super(message, cause);
  }

  public SecurityViolationException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
