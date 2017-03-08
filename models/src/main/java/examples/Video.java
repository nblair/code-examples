package examples;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Simple type describing a video.
 *
 * Similar to the "killrvideo" example provided in the DS220 class from the
 * <a href="https://academy.datastax.com/">DataStax Academy</a>.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Video video = (Video) o;

    if (videoId != null ? !videoId.equals(video.videoId) : video.videoId != null) {
      return false;
    }
    if (added != null ? !added.equals(video.added) : video.added != null) {
      return false;
    }
    if (title != null ? !title.equals(video.title) : video.title != null) {
      return false;
    }
    if (description != null ? !description.equals(video.description) : video.description != null) {
      return false;
    }
    return userId != null ? userId.equals(video.userId) : video.userId == null;
  }

  @Override
  public int hashCode() {
    int result = videoId != null ? videoId.hashCode() : 0;
    result = 31 * result + (added != null ? added.hashCode() : 0);
    result = 31 * result + (title != null ? title.hashCode() : 0);
    result = 31 * result + (description != null ? description.hashCode() : 0);
    result = 31 * result + (userId != null ? userId.hashCode() : 0);
    return result;
  }
}
