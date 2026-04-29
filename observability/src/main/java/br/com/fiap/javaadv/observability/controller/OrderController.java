package br.com.fiap.javaadv.observability.controller;


import br.com.fiap.javaadv.observability.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create() {
        return ResponseEntity.ok(orderService.createOrder());
    }

    @PutMapping("/{id}/process")
    public ResponseEntity<Map<String, Object>> process(@PathVariable long id) {
        return ResponseEntity.ok(orderService.processOrder(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> cancel(@PathVariable long id) {
        return ResponseEntity.ok(orderService.cancelOrder(id));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(orderService.getStats());
    }
}
