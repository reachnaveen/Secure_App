package com.example.secureapp;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExternalProductServiceTest {

    private WireMockServer wireMockServer;
    private RestTemplate restTemplate;

    @BeforeEach
    public void setup() {
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        configureFor("localhost", 8089);
        restTemplate = new RestTemplate();
    }

    @AfterEach
    public void teardown() {
        wireMockServer.stop();
    }

    @Test
    public void testGetProductFromExternalService() {
        stubFor(get(urlEqualTo("/external-products/1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"1\",\"name\":\"External Product\",\"price\":150.00}")));

        String product = restTemplate.getForObject("http://localhost:8089/external-products/1", String.class);

        assertEquals("{\"id\":\"1\",\"name\":\"External Product\",\"price\":150.00}", product);
    }
}
