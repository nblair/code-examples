package examples.datastax;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit tests for {@link FullTableScan}.
 *
 * @author https://github.com/nblair
 */
public class FullTableScanTest {

  @Test
  public void tokenColumn_control() {
    assertEquals("token(id)", control.tokenColumn());
  }
  @Test
  public void tokenColumn_compositeKey() {
    assertEquals("token(id1, id2)", compositeKey.tokenColumn());
  }

  /**
   * Given: table is empty
   * When: run tableScan()
   * Then: no results
   */
  @Test
  public void tableScan_empty() {
    AtomicInteger count = new AtomicInteger();
    Session session = mock(Session.class);
    ResultSet empty = mock(ResultSet.class);

    when(session.execute(any(Statement.class))).thenReturn(empty);
    when(empty.iterator()).thenReturn(Collections.<Row>emptyList().iterator());
    control.tableScan(session, v -> count.incrementAndGet());
    assertEquals(0, count.get());
  }

  /**
   * Given: table has 10 rows
   * When: run tableScan()
   * Then: one batch, observe 10 rows
   */
  @Test
  public void tableScan_onebatch() {
    AtomicInteger count = new AtomicInteger();
    Session session = mock(Session.class);
    ResultSet rs = mock(ResultSet.class);

    when(session.execute(any(Statement.class))).thenReturn(rs);
    Row row = mock(Row.class);
    when(rs.iterator())
      .thenReturn(new MockIterator(10, row))
      .thenReturn(Collections.<Row>emptyList().iterator());

    control.tableScan(session, v -> count.incrementAndGet());

    assertEquals(10, count.get());
    verify(session, times(2)).execute(any(Statement.class));
  }

  /**
   * Given: table has ~3x {@link FullTableScan#limit()} rows
   * When: run tableScan()
   * Then: observe 3 batches, observe all rows
   */
  @Test
  public void tableScan_multiplebatches() {
    AtomicInteger count = new AtomicInteger();
    Session session = mock(Session.class);
    ResultSet rs = mock(ResultSet.class);

    when(session.execute(any(Statement.class))).thenReturn(rs);
    Row row = mock(Row.class);

    when(row.getLong(control.keyspacePrefixedTokenColumn()))
      // end of batch 1
      .thenReturn(0L)
      // end of batch 2
      .thenReturn(20000L)
      // end of batch 3
      .thenReturn(50000L);

    when(rs.iterator())
      // result set 1
      .thenReturn(new MockIterator(control.limit(), row))
      // result set 2
      .thenReturn(new MockIterator(control.limit(), row))
      // result set 3
      .thenReturn(new MockIterator(control.limit() - 10, row))
      // result set 4 (is empty)
      .thenReturn(Collections.<Row>emptyList().iterator());

    control.tableScan(session, v -> count.incrementAndGet());

    assertEquals((control.limit() * 3) - 10, count.get());

    verify(session, times(4)).execute(any(Statement.class));
  }

  private final FullTableScan<String> control = new FullTableScan<String>() {
    @Override
    public String table() {
      return "control";
    }

    @Override
    public List<String> partitionKeys() {
      return ImmutableList.of("id");
    }

    @Override
    public String mapRow(Row row) {
      return row.getString("id");
    }
  };
  private final FullTableScan<String> compositeKey = new FullTableScan<String>() {
    @Override
    public String table() {
      return "foo";
    }

    @Override
    public List<String> partitionKeys() {
      return ImmutableList.of("id1", "id2");
    }

    @Override
    public String mapRow(Row row) {
      return row.getString("id1");
    }
  };

  /**
   * Test Iterator that returns the same row N times until the count is achieved.
   */
  static final class MockIterator implements Iterator<Row> {
    private final int max;
    private final Row row;
    private final AtomicInteger count = new AtomicInteger();

    MockIterator(int max, Row row) {
      this.max = max;
      this.row = row;
    }

    @Override
    public boolean hasNext() {
      return count.get() < max;
    }

    @Override
    public Row next() {
      if (hasNext()) {
        count.incrementAndGet();
        return row;
      }

      return null;
    }
  }

}
