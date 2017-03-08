package examples.client;

import feign.Response;
import feign.codec.ErrorDecoder;

/**
 * Created by nblair on 3/3/17.
 */
class VideoApiErrorDecoder implements ErrorDecoder {

  @Override
  public Exception decode(String methodKey, Response response) {
    return new VideoApiClientException(response.toString());
  }
}
