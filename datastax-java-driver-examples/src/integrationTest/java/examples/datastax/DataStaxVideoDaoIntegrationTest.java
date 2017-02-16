package examples.datastax;

import static org.junit.Assert.assertTrue;

import com.google.common.base.Stopwatch;
import examples.Video;
import examples.VideoDao;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Integration test for {@link DataStaxVideoDao}.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IntegrationTestConfiguration.class)
public class DataStaxVideoDaoIntegrationTest {

  private final Logger logger = LoggerFactory.getLogger(DataStaxVideoDaoIntegrationTest.class);
  @Autowired
  private VideoDao videoDao;

  @Test
  public void tableScan() {
    AtomicLong count = new AtomicLong();
    logger.info("starting full table scan on VideoDao");
    Stopwatch watch = Stopwatch.createStarted();
    videoDao.onEvery(v -> count.incrementAndGet());
    logger.info("counted {} records in {}", count.get(), watch.stop());
    assertTrue(count.get() > 1L);
  }
}
