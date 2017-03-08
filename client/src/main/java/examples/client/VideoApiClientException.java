package examples.client;

/**
 * {@link RuntimeException} for client issues.
 */
public class VideoApiClientException extends RuntimeException {

  public VideoApiClientException(String message) {
    super(message);
  }
  public VideoApiClientException(String message, Throwable cause) {
    super(message, cause);
  }
}
