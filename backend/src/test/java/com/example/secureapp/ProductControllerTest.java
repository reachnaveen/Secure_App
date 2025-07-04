package com.example.secureapp;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ProductControllerTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    public void setup() {
        RestAssured.port = port;
    }

    @Test
    public void testGetAllProducts() {
        given()
            .when()
                .get("/api/products")
            .then()
                .statusCode(200)
                .body("", hasSize(3));
    }

    @Test
    public void testGetProductById() {
        given()
            .when()
                .get("/api/products/1")
            .then()
                .statusCode(200)
                .body("name", equalTo("Laptop"));
    }
}
