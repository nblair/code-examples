package examples.datastax;

import com.datastax.driver.core.utils.UUIDs;
import com.github.javafaker.Faker;
import com.google.common.base.Stopwatch;
import examples.Video;
import examples.VideoDao;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;

/**
 * This bean can be used to insert a lot of random videos.
 *
 */
public class RandomVideoCreator {

  private final Logger logger = LoggerFactory.getLogger(RandomVideoCreator.class);
  @Autowired
  private VideoDao videoDao;
  @Autowired
  private Environment env;

  @Value("#{environment['test.randomVideoCreator.enabled'] ?: false}")
  private boolean enabled = false;
  @Value("#{environment['test.randomVideoCreator.rounds'] ?: 10}")
  private int rounds = 10;
  @Value("#{environment['test.randomVideoCreator.batchSize'] ?: 100}")
  private int batchSize = 100;
  private final Faker faker = new Faker();

  @PostConstruct
  public void populate() {
    if (this.enabled) {
      logger.warn("enabled; executing {} rounds with batchSize {}", this.rounds, this.batchSize);
      for (int i = 0; i < this.rounds; i++) {
        List<Video> videos = new ArrayList<Video>();
        for (int j = 0; j < this.batchSize; j++) {
          videos.add(new Video()
            .setVideoId(UUIDs.timeBased())
            .setAdded(faker.date().past(365, TimeUnit.DAYS).toInstant())
            .setDescription(faker.lorem().paragraph(2))
            .setTitle(faker.lorem().sentence())
            .setUserId(UUIDs.random()));
        }

        Stopwatch stopwatch = Stopwatch.createStarted();
        videoDao.save(videos);
        logger.info("completed round {}, save time: {}", i, stopwatch.stop());
      }
    }
  }
}
