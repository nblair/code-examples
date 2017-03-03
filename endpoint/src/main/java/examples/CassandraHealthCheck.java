package examples;

import com.codahale.metrics.health.HealthCheck;
import com.datastax.driver.core.Session;
import examples.EndpointConfiguration.CassandraConfiguration;
import java.util.Arrays;

/**
 * Basic {@link HealthCheck} for confirming cassandra availability
 */
class CassandraHealthCheck extends HealthCheck {

  private final Session session;
  private final CassandraConfiguration conf;

  CassandraHealthCheck(CassandraConfiguration conf, Session session) {
    this.session = session;
    this.conf = conf;
  }
  @Override
  protected Result check() throws Exception {
    try {
      session
        .executeAsync(conf.getValidationQuery())
        .get(conf.getValidationTimeout().getQuantity(), conf.getValidationTimeout().getUnit());
      return Result.healthy();
    } catch (Exception e) {
      return Result.unhealthy(
        "caught exception attempting to connect to %s, %s",
        Arrays.toString(conf.getContactPoints()),
        e);
    }
  }
}
