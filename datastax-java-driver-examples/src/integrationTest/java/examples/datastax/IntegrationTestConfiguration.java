package examples.datastax;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.extras.codecs.jdk8.InstantCodec;
import examples.VideoDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;

/**
 * {@link Configuration} for integration tests
 */
@Configuration
@Import(DaoConfiguration.class)
@PropertySource(value = "classpath:integration-test.properties", name = "conf")
public class IntegrationTestConfiguration {


  @Bean
  RandomVideoCreator creator() {
    return new RandomVideoCreator();
  }
}
