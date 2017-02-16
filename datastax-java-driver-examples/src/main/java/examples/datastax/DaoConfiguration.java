package examples.datastax;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.extras.codecs.jdk8.InstantCodec;
import examples.VideoDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Created by nblair on 2/16/17.
 */
@Configuration
public class DaoConfiguration {

  @Autowired
  private Environment env;

  @Bean
  Session cassandraSession() {
    String [] contactPoints = env.getProperty("cassandra.contactPoints", String[].class);
    Cluster cluster = Cluster.builder()
      .addContactPoints(contactPoints)
      .build();
    cluster.getConfiguration().getCodecRegistry().register(InstantCodec.instance);
    Session session = cluster.connect(env.getProperty("cassandra.keyspace", "examples"));

    return session;
  }

  @Bean @Autowired
  VideoDao datastaxVideoDao(Session session) {
    return new DataStaxVideoDao(session);
  }
}
