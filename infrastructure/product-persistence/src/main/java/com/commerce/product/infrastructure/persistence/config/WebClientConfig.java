package com.commerce.product.infrastructure.persistence.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * WebClient 설정
 * 다른 마이크로서비스와의 통신을 위한 WebClient 빈을 설정합니다.
 */
@Configuration
public class WebClientConfig {
    
    @Value("${inventory.service.url:http://inventory-service}")
    private String inventoryServiceUrl;
    
    @Value("${webclient.timeout.connect:5000}")
    private int connectTimeout;
    
    @Value("${webclient.timeout.read:5000}")
    private int readTimeout;
    
    @Value("${webclient.timeout.write:5000}")
    private int writeTimeout;
    
    @Value("${webclient.buffer.size:1048576}")  // 1MB
    private int bufferSize;
    
    /**
     * Inventory 서비스와 통신하기 위한 WebClient
     */
    @Bean
    public WebClient inventoryServiceWebClient(WebClient.Builder webClientBuilder) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                .responseTimeout(Duration.ofMillis(readTimeout))
                .doOnConnected(conn -> 
                    conn.addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(writeTimeout, TimeUnit.MILLISECONDS)));
        
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(bufferSize))
                .build();
        
        return webClientBuilder
                .baseUrl(inventoryServiceUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(exchangeStrategies)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}