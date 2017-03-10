package examples;

import com.codahale.metrics.health.HealthCheck;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.hash.BloomFilter;
import com.google.common.io.CountingOutputStream;
import examples.resources.VideoBloomFilterManager;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

/**
 * {@link HealthCheck} for the {@link VideoBloomFilterManager}.
 */
public class BloomFilterManagerHealthCheck extends HealthCheck {

  private final VideoBloomFilterManager manager;
  private final ObjectMapper mapper;

  public BloomFilterManagerHealthCheck(VideoBloomFilterManager manager, ObjectMapper mapper) {
    this.manager = manager;
    this.mapper = mapper;
  }

  /**
   * {@inheritDoc}
   * Note: this will return unhealthy while {@link VideoBloomFilterManager#getVideoIdFilter()} returns null.
   * This means healthchecks will fail after startup until the first bloomfilter
   * is populated.
   */
  @Override
  protected Result check() throws Exception {
    BloomFilter<String> filter = manager.getVideoIdFilter();
    if(filter == null) {
      return Result.unhealthy("videoId bloomFilter not available");
    }
    HashMap<String, Object> message = Maps.newHashMap();
    CountingOutputStream stream = new CountingOutputStream(new OutputStream() {
      @Override
      public void write(int b) throws IOException {
        // no-op; we don't care about the bytes, just the count provided by the wrapping CountingOutputStream
      }
    });
    filter.writeTo(stream);
    message.put("lastUpdated", manager.getLastUpdated());
    message.put("sizeInBytes", stream.getCount());
    message.put("updateThreshold", manager.getUpdateThreshold());
    return Result.healthy(mapper.writeValueAsString(message));
  }
}
