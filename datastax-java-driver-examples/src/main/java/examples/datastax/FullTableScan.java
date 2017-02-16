package examples.datastax;

import static com.datastax.driver.core.querybuilder.QueryBuilder.gte;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Mixin style interface to provide the boiler plate around a full Cassandra table scan using the
 * DataStax Java driver.
 *
 * @author https://github.com/nblair
 */
public interface FullTableScan<T> {

  /**
   *
   * @return the name of the table (column family) to query, must not be null
   */
  String table();

  /**
   *
   * @return a list containing the names of the columns that compose the partition key; must not be empty
   */
  List<String> partitionKeys();

  /**
   *
   * @param row a single row in the result set
   * @return a corresponding object that can be hydrated from the row, must not be null
   */
  T mapRow(Row row);

  /**
   *
   * @return the name of the keyspace, or null if we should just use the keyspace of the provided {@link Session}
   */
  default String keyspace() {
    return null;
  }

  /**
   *
   * @return additional columns needed by {@link #mapRow(Row)}
   */
  default List<String> columns() {
    return Collections.emptyList();
  }

  /**
   *
   * @return the number of rows to retrieve at each iteration
   */
  default int limit() {
    return 10000;
  }

  default ConsistencyLevel consistencyLevel() {
    return ConsistencyLevel.ALL;
  }
  /**
   *
   * @return a string representing the "token(...)" column
   */
  default String tokenColumn() {
    StringBuilder clause = new StringBuilder("token(");
    for(Iterator<String> keys = partitionKeys().iterator(); keys.hasNext(); ) {
      clause.append(keys.next());
      if (keys.hasNext()) {
        clause.append(", ");
      }
    }
    clause.append(")");
    return clause.toString();
  }

  /**
   * Used internally by {@link #tableScan(Session, Consumer)} to pull the current token value
   * with {@link Row#getLong(String)}.
   *
   * @return {@link #tokenColumn()}, but prefixed with the keyspace "system"
   */
  default String keyspacePrefixedTokenColumn() {
    return "system." + tokenColumn();
  }

  /**
   * Perform a full table scan, executing the provided consumer on each row.
   *
   * @param session an established C* session
   * @param consumer the consumer to act on each row
   */
  default void tableScan(Session session, Consumer<T> consumer) {
    // this is the minimum possible value returned by the token function (given Murmur3 partitioner)
    Long currentToken = -9223372036854775808L;
    Long maxToken = currentToken;

    boolean maxTokenReached = false;
    while (!maxTokenReached) {
      ResultSet resultSet = session.execute(statement(currentToken));
      Iterator<Row> rows = resultSet.iterator();

      if ( !rows.hasNext()){
        maxTokenReached = true;
      } else {
        while (rows.hasNext()) {
          Row row = rows.next();
          consumer.accept(mapRow(row));

          if (!rows.hasNext()) {
            // reached the end of the result set, snag the token value
            currentToken = row.getLong(keyspacePrefixedTokenColumn());
            if (currentToken > maxToken) {
              maxToken = currentToken;
              currentToken++;
            }
          }
        }
      }
    }
  }

  /**
   *
   * @param currentTokenValue the current value of the token
   * @return a statement
   */
  default Statement statement(Long currentTokenValue) {
    Select.Selection select = QueryBuilder.select()
      .column(tokenColumn());

    partitionKeys().forEach(k -> select.column(k));
    columns().forEach(c -> select.column(c));

    return select
      .from(keyspace(), table())
      .where(gte(tokenColumn(), currentTokenValue))
      .limit(limit())
      .setConsistencyLevel(consistencyLevel());
  }
}
