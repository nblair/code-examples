package examples.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.base.Charsets;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;
import examples.Video;
import examples.VideoApi;
import feign.Feign;
import feign.RequestLine;
import feign.Response;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.jaxrs.JAXRSContract;
import java.io.IOException;

/**
 * Feign backed {@link VideoApi} client.
 */
public class VideoApiClient {

  private final VideoApi api;
  private final VideoMeta metaApi;
  private final ObjectMapper mapper;
  /**
   *
   * @param baseUri base uri for videos endpoint, e.g. 'http://localhost:8080/videos'
   */
  public VideoApiClient(String baseUri) {
    this.mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    this.api = Feign.builder()
      .contract(new JAXRSContract())
      .decode404()
      .decoder(new JacksonDecoder(mapper))
      .encoder(new JacksonEncoder(mapper))
      .errorDecoder(new VideoApiErrorDecoder())
      .target(VideoApi.class, baseUri);

    this.metaApi = Feign.builder()
      .target(VideoMeta.class, baseUri);
  }

  /**
   *
   * @param videoId the target {@link Video#getVideoId()}
   * @return the corresponding {@link Video}, or null
   */
  public Video get(String videoId) {
    return api.get(videoId);
  }

  /**
   * @param video the {@link Video} to create
   * @return the created {@link Video}
   */
  public Video create(Video video) {
    return api.create(video);
  }

  /**
   * @param video the {@link Video} to update
   * @return the updated {@link Video}
   */
  public Video update(Video video) {
    return api.update(video);
  }

  /**
   * @return a {@link BloomFilter} of known {@link Video#getVideoId()}s, or null if not yet available
   */
  public BloomFilter<String> getBloomFilter() {
    Response response = metaApi.getBloomFilter();
    if(response.status() == 204) {
      return null;
    }
    try {
      // note: we can't use Funnels.stringFunnel because of the type mismatch (it returns Funnel<CharSequence>)
      BloomFilter<String> filter = BloomFilter.readFrom(
        response.body().asInputStream(),
        new Funnel<String>() {
          @Override
          public void funnel(String from, PrimitiveSink into) {
            into.putString(from, Charsets.UTF_8);
          }
        });
      return filter;
    } catch (IOException e) {
      throw new VideoApiClientException("caught IOException in getBloomFilter", e);
    }
  }

  /**
   * Request that the bloomFilter returned by {@link #getBloomFilter()} be updated server side.
   * Building the bloomFilter is a resource intensive operation that happens asynchronously.
   * There is no time guarantee regarding when {@link #getBloomFilter()} will be returning new data.
   *
   * @return true if the request was accepted, false otherwise
   */
  public boolean updateBloomFilter() {
    Response response = metaApi.updateBloomFilter();
    return 200 == response.status();
  }

  /**
   * Interface describing video meta; to be targeted by {@link Feign.Builder#target(Class, String)}.
   */
  public interface VideoMeta {

    @RequestLine("GET /bloomFilter")
    Response getBloomFilter();

    @RequestLine("POST /bloomFilter")
    Response updateBloomFilter();
  }
}
