package examples;

import com.codahale.metrics.health.HealthCheck;
import examples.resources.VideoBloomFilterManager;

/**
 * {@link HealthCheck} for the {@link VideoBloomFilterManager}.
 */
public class BloomFilterManagerHealthCheck extends HealthCheck {

  private final VideoBloomFilterManager manager;

  public BloomFilterManagerHealthCheck(VideoBloomFilterManager manager) {
    this.manager = manager;
  }

  /**
   * {@inheritDoc}
   * Note: this will return unhealthy while {@link VideoBloomFilterManager#getVideoIdFilter()} returns null.
   * This means healthchecks will fail after startup until the first bloomfilter
   * is populated.
   */
  @Override
  protected Result check() throws Exception {
    if(manager.getVideoIdFilter() == null) {
      return Result.unhealthy("videoId bloomFilter not available");
    }
    return Result.healthy("videoId bloomFilter last built at %s, can be updated after %s", manager.getLastUpdated(), manager.getUpdateThreshold());
  }
}
