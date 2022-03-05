package com.bdo.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DbConfig extends DefaultDbConfig {

    @Bean
    @Qualifier("bot-db")
    @ConfigurationProperties(prefix = "db")
    SpringDataJdbcProperties gitlabJdbcProperties() {
        return new SpringDataJdbcProperties();
    }

    @Bean
    @Qualifier("bot-db")
    public DataSource gitlabDataSource(@Qualifier("bot-db") SpringDataJdbcProperties properties) {
        return hikariDataSource("db", properties);
    }

    @Bean
    @Qualifier("bot-db")
    JdbcTemplate gitlabJdbcTemplate(@Qualifier("bot-db") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Data
    @NoArgsConstructor
    public static class SpringDataJdbcProperties {

        String url;
        String driver;
        String poolSize;
        int minPoolSize = 4;
        int maxPoolSize = 10;

        /**
         * All-args constructor for {@link SpringDataJdbcProperties#toString()} (logging)
         *
         * @param url JDBC driver class name property
         * @param driver JDBC driver class name property
         * @param poolSize Hikari / Vertica maxPoolSize property
         */
        public SpringDataJdbcProperties(
                String url, String driver, String poolSize) {
            this.url = url;
            this.driver = driver;
            this.poolSize = poolSize;
        }
    }
}
