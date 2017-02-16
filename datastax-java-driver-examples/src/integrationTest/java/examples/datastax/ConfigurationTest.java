package examples.datastax;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import java.util.Iterator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Simple test to vet {@link IntegrationTestConfiguration}
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IntegrationTestConfiguration.class )
public class ConfigurationTest {

  @Autowired
  private Session session;

  @Test
  public void connect_success() {
    Statement select = QueryBuilder.select()
      .column("release_version")
      .from("system", "local");

    ResultSet rs = session.execute(select);
    Iterator<Row> rows = rs.iterator();
    assertTrue(rows.hasNext());
    Row row = rows.next();
    assertNotNull(row.getString("release_version"));
  }
}
