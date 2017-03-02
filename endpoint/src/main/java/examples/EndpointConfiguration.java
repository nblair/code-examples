package examples;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.util.Duration;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Endpoint configuration.
 */
public class EndpointConfiguration extends Configuration {

  @JsonProperty("cassandra")
  public CassandraConfiguration cassandra;

  @JsonProperty("swagger")
  public SwaggerBundleConfiguration swagger;

  /**
   * Container for cassandra related config.
   */
  public static class CassandraConfiguration {
    @NotEmpty
    @JsonProperty
    private String[] contactPoints;
    @JsonProperty
    private String keyspace = "examples";
    @JsonProperty
    private String validationQuery = "SELECT now() FROM system.local;";
    @JsonProperty
    private Duration validationTimeout = Duration.seconds(2L);

    public String[] getContactPoints() {
      return contactPoints;
    }

    public void setContactPoints(String[] contactPoints) {
      this.contactPoints = contactPoints;
    }

    public String getKeyspace() {
      return keyspace;
    }

    public void setKeyspace(String keyspace) {
      this.keyspace = keyspace;
    }

    public String getValidationQuery() {
      return validationQuery;
    }

    public void setValidationQuery(String validationQuery) {
      this.validationQuery = validationQuery;
    }

    public Duration getValidationTimeout() {
      return validationTimeout;
    }

    public void setValidationTimeout(Duration validationTimeout) {
      this.validationTimeout = validationTimeout;
    }
  }
}
