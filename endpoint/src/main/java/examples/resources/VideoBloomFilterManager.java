package examples.resources;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Charsets;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import com.google.common.hash.Funnels;
import com.google.common.hash.PrimitiveSink;
import com.google.common.util.concurrent.Futures;
import examples.Video;
import examples.VideoDao;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.util.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class responsible for building and tracking {@link BloomFilter}s for {@link Video}s.
 */
public class VideoBloomFilterManager implements Managed {

  private final Logger logger = LoggerFactory.getLogger(VideoBloomFilterManager.class);
  private final VideoDao videoDao;
  private final Duration updateFrequency;
  private int expectedInsertions;
  private double falsePositiveProbability;
  private BloomFilter<String> videoIdFilter;
  private volatile Instant last = Instant.now();
  private ExecutorService executorService = Executors.newSingleThreadExecutor();
  private final Timer timer;
  private Future<?> running = Futures.immediateFuture(null);

  /**
   *
   * @param videoDao
   * @param updateFrequency
   * @param roughDatasetSize
   * @param falsePositiveProbability
   */
  public VideoBloomFilterManager(VideoDao videoDao, Duration updateFrequency, int roughDatasetSize, double falsePositiveProbability, MetricRegistry metrics) {
    this.videoDao = videoDao;
    this.updateFrequency = updateFrequency;
    this.expectedInsertions = roughDatasetSize;
    this.falsePositiveProbability = falsePositiveProbability;
    this.timer = metrics.timer("buildVideoIdBloomFilter");
  }

  /**
   * Construct a {@link BloomFilter} containing every {@link Video#getVideoId()} value.
   */
  void build() {
    running = executorService.submit(() -> {
      logger.info("building new bloomfilter");
      final Timer.Context context = timer.time();
      try {
        BloomFilter<String> newFilter = BloomFilter.create(
          Funnels.stringFunnel(Charsets.UTF_8),
          expectedInsertions,
          falsePositiveProbability);

        videoDao.onEvery(v -> {
          newFilter.put(v.getVideoId().toString());
        });
        last = Instant.now();

        this.videoIdFilter = newFilter;
      } finally {
        context.stop();
        logger.info("new bloomfilter complete as of {}", last);
      }
    });
  }

  /**
   *
   * @return the current {@link BloomFilter}, or null if it hasn't been initialized yet
   */
  public BloomFilter<String> getVideoIdFilter() {
    return this.videoIdFilter;
  }

  /**
   * @return the time when the bloom filter was last updated, or null if hasn't run yet
   */
  public Instant getLastUpdated() {
    return last;
  }

  /**
   * @return the earliest possible time when {@link #updateFilter()} can be run successfully
   */
  public Instant getUpdateThreshold() {
    return last.plusMillis(updateFrequency.toMilliseconds());
  }
  /**
   * Updates the filter returned by {@link #getVideoIdFilter()}.
   * Since updating is really expensive, there is a time constraint on how frequently we can do this.
   *
   * @return true if the update is started; false otherwise
   */
  public synchronized boolean updateFilter() {
    // checking for null guarantees first invocation has to be performed by Dropwizard (via Managed#start)
    if (running.isDone() && last != null) {
      Instant threshold = last.plusMillis(updateFrequency.toMilliseconds());
      if (Instant.now().isAfter(getUpdateThreshold())) {
        build();
      }
    }
    return false;
  }

  @Override
  @Timed
  public void start() throws Exception {
    // directly invokes build to skip the comparision to updateFrequency
    build();
  }

  @Override
  public void stop() throws Exception {
    this.executorService.shutdownNow();
  }
}
