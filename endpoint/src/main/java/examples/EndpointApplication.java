package examples;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.extras.codecs.jdk8.InstantCodec;
import com.fasterxml.jackson.databind.SerializationFeature;
import examples.EndpointConfiguration.CassandraConfiguration;
import examples.datastax.DataStaxVideoDao;
import examples.resources.IllegalArgumentExceptionMapper;
import examples.resources.VideoBloomFilterManager;
import examples.resources.VideoResource;
import io.dropwizard.Application;
import io.dropwizard.configuration.ResourceConfigurationSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

/**
 * Entry point for the endpoint application.
 */
public class EndpointApplication extends Application<EndpointConfiguration> {

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      args = new String[] { "server", "application-defaults.yml"};
    }
    new EndpointApplication().run(args);
  }

  @Override
  public void run(EndpointConfiguration configuration, Environment environment) throws Exception {
    CassandraHealthCheck cassandraHealthCheck = new CassandraHealthCheck(configuration.cassandra, session(configuration.cassandra));
    environment.healthChecks().register("cassandra", cassandraHealthCheck);

    environment.jersey().register(new IllegalArgumentExceptionMapper(environment.metrics()));

    final VideoDao videoDao = videoDao(configuration);
    // bloom filter manager
    VideoBloomFilterManager videoBloomFilterManager = new VideoBloomFilterManager(
      videoDao,
      configuration.videos.getUpdateFrequency(),
      configuration.videos.getRoughDatasetSize(),
      configuration.videos.getBloomFilterFalsePositivePercentage(),
      environment.metrics());

    environment.lifecycle().manage(videoBloomFilterManager);

    // REST API
    VideoResource resource = new VideoResource(videoDao, videoBloomFilterManager);
    environment.jersey().register(resource);
  }

  @Override
  public void initialize(Bootstrap<EndpointConfiguration> bootstrap) {
    // supports loading of classpath configuration
    bootstrap.setConfigurationSourceProvider(new ResourceConfigurationSourceProvider());

    bootstrap.getObjectMapper().configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    bootstrap.addBundle(new SwaggerBundle<EndpointConfiguration>() {
      @Override
      protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(
        EndpointConfiguration configuration) {
        return configuration.swagger;
      }
    });
  }

  protected Session session(CassandraConfiguration conf) {
    Cluster cluster = Cluster.builder()
      .addContactPoints(conf.getContactPoints())
      .build();
    cluster.getConfiguration().getCodecRegistry().register(InstantCodec.instance);
    Session session = cluster.connect(conf.getKeyspace());
    return session;
  }
  protected VideoDao videoDao(EndpointConfiguration configuration) {
    return new DataStaxVideoDao(session(configuration.cassandra));
  }
}
