package examples.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.common.hash.BloomFilter;
import examples.Video;
import java.time.Instant;
import java.util.UUID;
import org.junit.Test;

/**
 * Integration tests for {@link VideoApiClient}.
 */
public class VideoApiClientIntegrationTest {

  public static final String KNOWN_VIDEO_ID = "d01b60a1-f3c9-11e6-b932-9956dc3f8a66";
  private final VideoApiClient client = new VideoApiClient("http://localhost:8080/videos");
  @Test
  public void get_successful() {
    Video video = client.get(KNOWN_VIDEO_ID);
    assertEquals(UUID.fromString(KNOWN_VIDEO_ID), video.getVideoId());
  }
  @Test(expected = VideoApiClientException.class)
  public void get_invalid_id() {
    client.get("foo");
  }
  @Test
  public void get_nonexistent() {
    assertNull(client.get("d01b60a1-f3c9-11e6-b932-9956dc3f8a67"));
  }
  @Test
  public void create_successful() {
    Video video = new Video()
      .setTitle("create_successful")
      .setDescription("integration test on " + Instant.now())
      .setAdded(Instant.now())
      .setUserId(UUID.randomUUID());

    Video returned = client.create(video);
    assertNotNull(returned.getVideoId());
    assertEquals(video.getTitle(), returned.getTitle());
    assertEquals(video.getAdded(), returned.getAdded());
    assertEquals(video.getUserId(), returned.getUserId());
  }
  @Test
  public void getBloomFilter_successful() {
    BloomFilter<String> filter = client.getBloomFilter();
    assertNotNull(filter);
    assertTrue(filter.mightContain(KNOWN_VIDEO_ID));
  }
}
