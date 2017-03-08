package examples.datastax;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.utils.UUIDs;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import examples.Video;
import examples.VideoDao;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Datastax Java driver backed {@link VideoDao}.
 */
@Singleton
public class DataStaxVideoDao implements VideoDao, FullTableScan<Video> {

  private final Logger logger = LoggerFactory.getLogger(DataStaxVideoDao.class);
  private final Session session;
  private final String keyspace;
  private final String table;
  private final int batchSize;

  /**
   * Defaults to keyspace of "examples", table name of "videos", and a batchsize of 100.
   *
   * @param session live datastax session
   */
  public DataStaxVideoDao(Session session) {
    this(session, "examples", "videos", 100);
  }

  /**
   *
   * @param session live datastax session
   * @param keyspace the name of the keyspace where the table resides
   * @param table the name of the table
   * @param batchSize limit on batch size for {@link #save(Collection)}
   */
  public DataStaxVideoDao(Session session, String keyspace, String table, int batchSize) {
    this.session = session;
    this.keyspace = keyspace;
    this.table = table;
    this.batchSize = batchSize;
  }

  @Override
  public Collection<Video> save(Collection<Video> videos) {
    List<Video> saved = new ArrayList();
    if(videos.size() == 1) {
      // don't batch if only one video
      Video video = videos.iterator().next();
      final UUID uuid = video.getVideoId() == null ? UUIDs.timeBased() : video.getVideoId();
      if(session.execute(insert(video.setVideoId(uuid), uuid)).wasApplied()) {
        saved.add(video);
      };
    } else {
      for (Iterator<List<Video>> iterator = Iterators.partition(videos.iterator(), batchSize);
        iterator.hasNext(); ) {
        List<Video> chunk = iterator.next();
        if (!chunk.isEmpty()) {
          BatchStatement batch = new BatchStatement();
          for (Video v : chunk) {
            // if no videoId present, generate a new timeuuid
            final UUID uuid = getOrGenerateUUID(v);
            batch.add(insert(v, uuid));
          }
          if(session.execute(batch).wasApplied()) {
            saved.addAll(chunk);
          } else {
            logger.error("batch was not successfully applied during save (enable debug to see failed inserts)");
            logger.debug("batch was not successfully applied during save, affected videoIds: {}",
              chunk
                .stream()
                .map(v -> v.getVideoId())
                .collect(Collectors.toList()));
          }
        }
      }
    }
    return saved;
  }

  @Override
  public Video retrieve(UUID videoId) {
    Statement select = QueryBuilder.select().all().from(keyspace, table)
      .where(eq("video_id", videoId));
    ResultSet rs = session.execute(select);
    Row row = rs.one();
    if(row != null) {
      return mapRow(row);
    }
    return null;
  }

  @Override
  public void onEvery(Consumer<Video> function) {
    tableScan(this.session, function);
  }

  /**
   *
   * @param v the video to store
   * @param uuid the value to use for the video_id column
   * @return an INSERT statement to store the video
   */
  protected Statement insert(Video v, UUID uuid) {
    return QueryBuilder.insertInto(keyspace, table)
      .value("video_id", uuid)
      .value("added_date", v.getAdded())
      .value("description", v.getDescription())
      .value("title", v.getTitle())
      .value("user_id", v.getUserId());
  }

  /**
   * Potentially mutative method.
   * Check if {@link Video#getVideoId()} is present; if true, return it and do not modify the video.
   * If {@link Video#getVideoId()} is null, generate a new timeuuid, and pass it to {@link Video#setVideoId(UUID)}.
   *
   * @param v the video to inspect
   * @return the resulting value of {@link Video#getVideoId()} (never null)
   */
  protected UUID getOrGenerateUUID(Video v) {
    if(v.getVideoId() != null) {
      return v.getVideoId();
    }

    UUID newValue = UUIDs.timeBased();
    v.setVideoId(newValue);
    return newValue;
  }

  @Override
  public String table() {
    return this.table;
  }

  @Override
  public List<String> partitionKeys() {
    return ImmutableList.of("video_id");
  }

  @Override
  public Video mapRow(Row row) {
    return new Video()
      .setVideoId(row.getUUID("video_id"))
      .setAdded(row.getTimestamp("added_date").toInstant())
      .setDescription(row.getString("description"))
      .setTitle(row.getString("title"))
      .setUserId(row.getUUID("user_id"));
  }

  @Override
  public String keyspace() {
    return this.keyspace;
  }

  @Override
  public List<String> columns() {
    return ImmutableList.of("added_date", "description", "title", "user_id");
  }
}
