package examples;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Simple data access interface for {@link Video}s
 */
public interface VideoDao {

  /**
   * Save a single video
   * @param video the video to save (not null)
   */
  default void save(Video video) {
    save(ImmutableList.of(video));
  }

  /**
   * Save a collection of videos.
   * @param videos
   */
  void save(Collection<Video> videos);

  /**
   *
   * @param videoId the target {@link Video#getVideoId()}
   * @return the matching {@link Video}, or null
   */
  Video retrieve(UUID videoId);

  /**
   * WARNING: this is an expensive operation.
   *
   * Run {@link Consumer#accept(Object)} on each and every stored {@link Video}.
   *
   * @param function the consumer to run on each
   */
  void onEvery(Consumer<Video> function);
}
