package examples.datastax;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import examples.Video;
import examples.VideoDao;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Created by nblair on 2/15/17.
 */
public class DataStaxVideoDao implements VideoDao, FullTableScan<Video> {

  private final Session session;
  private final String keyspace;
  private final String table;
  private final int batchSize;

  /**
   *
   * @param session
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
  public void save(Collection<Video> videos) {
    if(videos.size() == 1) {
      // don't batch if only one video
      session.execute(insert(videos.iterator().next()));
    } else {
      for (Iterator<List<Video>> iterator = Iterators.partition(videos.iterator(), batchSize);
        iterator.hasNext(); ) {
        List<Video> chunk = iterator.next();
        if (!chunk.isEmpty()) {
          BatchStatement batch = new BatchStatement();
          for (Video v : chunk) {
            batch.add(insert(v));
          }
          session.execute(batch);
        }
      }
    }
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
   * @return an INSERT statement to store the video
   */
  protected Statement insert(Video v) {
    return QueryBuilder.insertInto(keyspace, table)
      .value("video_id", v.getVideoId())
      .value("added_date", v.getAdded())
      .value("description", v.getDescription())
      .value("title", v.getTitle())
      .value("user_id", v.getUserId());
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
