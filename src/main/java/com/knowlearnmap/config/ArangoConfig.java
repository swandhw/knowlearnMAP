package com.knowlearnmap.config;

import com.arangodb.ArangoDB;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class ArangoConfig {

    @Value("${arangodb.host}")
    private String host;

    @Value("${arangodb.port}")
    private int port;

    @Value("${arangodb.user}")
    private String user;

    @Value("${arangodb.password}")
    private String password;

    @Value("${arangodb.max-connections:10}")
    private int maxConnections;

    @Bean
    public ArangoDB arangoDB() {
        log.info("Initializing ArangoDB connection to {}:{}", host, port);
        return new ArangoDB.Builder()
                .host(host, port)
                .user(user)
                .password(password)
                .maxConnections(maxConnections)

                .build();
    }
}
