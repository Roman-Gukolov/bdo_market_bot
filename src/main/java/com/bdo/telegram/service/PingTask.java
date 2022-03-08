package com.bdo.telegram.service;

import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Task for Heroku. Should provide app not too sleep after 30 min.
 */
@Component
@NoArgsConstructor
@Slf4j
public class PingTask {
    private final static String PING_URL = "https://www.google.com";

    @Scheduled(fixedRate = 300000)
    @Async
    @SneakyThrows
    public void ping() {
        URL url = new URL(PING_URL);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.connect();
        log.info("Ping {}, OK: response code {}", url.getHost(), con.getResponseCode());
        con.disconnect();
    }
}
