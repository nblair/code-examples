package examples.client;

import static org.junit.Assert.assertNotNull;

import com.datastax.driver.core.utils.UUIDs;
import com.google.common.base.Stopwatch;
import com.google.common.hash.BloomFilter;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This integration test is intended to demonstrate the impact of using a bloom filter on the client
 * side to avoid making unnecessary network roundtrips.
 */
public class BloomFilterDemonstrationIntegrationTest {

  public static final int DATASET_SIZE = 50_000;
  public static final String KNOWN_VIDEO_ID = "d01b60a1-f3c9-11e6-b932-9956dc3f8a66";
  private static final Set<String> arguments = new HashSet<>();
  private final Logger logger = LoggerFactory.getLogger(BloomFilterDemonstrationIntegrationTest.class);
  private final VideoApiClient client = new VideoApiClient("http://localhost:8080/videos");
  private BloomFilter<String> filter;

  @BeforeClass
  public static void createIds() {

    // use datastax driver UUIDs utility to generate valid timeuuid values
    for (int i = 0; i < DATASET_SIZE; i++) {
      arguments.add(UUIDs.timeBased().toString());
    }
    // known actual video id is added to guarantee us 1 client.get() success for both tests.
    arguments.add(KNOWN_VIDEO_ID);
  }
  @Before
  public void getBloomFilter() {
    this.filter = client.getBloomFilter();
    assertNotNull("bloomfilter not available, re-run test when available", this.filter);
  }

  /**
   * Run through the id arguments, in parallel, executing {@link VideoApiClient#get(String)} on each.
   * Check INFO logs for time results.
   */
  @Test
  public void without() {
    AtomicInteger matches = new AtomicInteger();
    Stopwatch stopwatch = Stopwatch.createStarted();
    arguments.parallelStream().forEach(id -> {
      if(client.get(id) != null) {
        matches.incrementAndGet();
      }
    });
    stopwatch.stop();
    logger.info("client without filter executed {} total gets, with {} successful get, in {}", arguments.size(), matches.get(), stopwatch);
  }

  /**
   * Run through the id arguments, in parallel, but check {@link BloomFilter#mightContain(Object)} on the
   * id before executing {@link VideoApiClient#get(String)}.
   * Check INFO logs for time results.
   */
  @Test
  public void with() {
    AtomicInteger matches = new AtomicInteger();
    AtomicInteger skip = new AtomicInteger();
    AtomicInteger falsePositives = new AtomicInteger();
    Stopwatch stopwatch = Stopwatch.createStarted();
    arguments.parallelStream().forEach(id -> {
      if(filter.mightContain(id)) {
        if (client.get(id) != null) {
          matches.incrementAndGet();
        } else {
          falsePositives.incrementAndGet();
        }
      } else {
        skip.incrementAndGet();
      }
    });
    stopwatch.stop();
    logger.info("client with filter skipped {} gets, made {} unsuccessful gets (filter false positives), {} successful get, in {}", skip.get(), falsePositives.get(), matches.get(), stopwatch);
  }
}
