package examples;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Simple type describing a video.
 *
 * Similar to the "killrvideo" example provided in the DS220 class.
 */
public class Video {

  private UUID videoId;
  private Instant added;
  private String title;
  private String description;
  private UUID userId;

  public UUID getVideoId() {
    return videoId;
  }

  public Video setVideoId(UUID videoId) {
    this.videoId = videoId;
    return this;
  }

  public Instant getAdded() {
    return added;
  }

  public Video setAdded(Instant added) {
    this.added = added;
    return this;
  }

  public String getTitle() {
    return title;
  }

  public Video setTitle(String title) {
    this.title = title;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public Video setDescription(String description) {
    this.description = description;
    return this;
  }

  public UUID getUserId() {
    return userId;
  }

  public Video setUserId(UUID userId) {
    this.userId = userId;
    return this;
  }
}
