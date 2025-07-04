package com.example.secureapp;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final List<Product> products = Arrays.asList(
        new Product("1", "Laptop", 1200.00),
        new Product("2", "Keyboard", 75.00),
        new Product("3", "Mouse", 25.00)
    );

    @GetMapping
    public List<Product> getAllProducts() {
        return products;
    }

    @GetMapping("/{id}")
    public Product getProductById(@PathVariable String id) {
        return products.stream()
            .filter(product -> product.getId().equals(id))
            .findFirst()
            .orElse(null);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Product createProduct(@Valid @RequestBody Product newProduct) {
        // In a real application, you would save this to a database
        System.out.println("Creating product: " + newProduct.getName());
        return newProduct;
    }
}
