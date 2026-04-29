package br.com.fiap.javaadv.observability.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class OrderService {

    private final Counter ordersCreatedCounter;
    private final Counter ordersCancelledCounter;
    private final Timer orderProcessingTimer;
    private final AtomicInteger pendingOrders = new AtomicInteger(0);
    private final AtomicLong totalRevenue = new AtomicLong(0);
    private final Map<String, AtomicInteger> ordersByStatus = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public OrderService(MeterRegistry registry) {
        // Contador de pedidos criados
        this.ordersCreatedCounter = Counter.builder("orders.created.total")
                .description("Total de pedidos criados")
                .register(registry);

        // Contador de pedidos cancelados
        this.ordersCancelledCounter = Counter.builder("orders.cancelled.total")
                .description("Total de pedidos cancelados")
                .register(registry);

        // Timer para processamento de pedidos
        this.orderProcessingTimer = Timer.builder("orders.processing.duration")
                .description("Tempo de processamento dos pedidos")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);

        // Gauge de pedidos pendentes (valor atual)
        Gauge.builder("orders.pending.current", pendingOrders, AtomicInteger::get)
                .description("Pedidos pendentes no momento")
                .register(registry);

        // Gauge de receita total
        Gauge.builder("orders.revenue.total", totalRevenue, v -> v.get() / 100.0)
                .description("Receita total acumulada em reais")
                .register(registry);

        // Inicializa status map
        ordersByStatus.put("PENDING", new AtomicInteger(0));
        ordersByStatus.put("PROCESSING", new AtomicInteger(0));
        ordersByStatus.put("COMPLETED", new AtomicInteger(0));
        ordersByStatus.put("CANCELLED", new AtomicInteger(0));

        // Gauge por status
        ordersByStatus.forEach((status, count) ->
                Gauge.builder("orders.by.status", count, AtomicInteger::get)
                        .tag("status", status)
                        .description("Pedidos agrupados por status")
                        .register(registry)
        );
    }

    public Map<String, Object> createOrder() {
        return orderProcessingTimer.record(() -> {
            // Simula processamento (50-300ms)
            try {
                Thread.sleep(50 + random.nextInt(250));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            ordersCreatedCounter.increment();
            pendingOrders.incrementAndGet();
            ordersByStatus.get("PENDING").incrementAndGet();

            int valor = 1000 + random.nextInt(50000); // R$ 10,00 a R$ 500,00
            totalRevenue.addAndGet(valor);

            long orderId = System.currentTimeMillis();
            return Map.of(
                    "orderId", orderId,
                    "status", "PENDING",
                    "valorCentavos", valor,
                    "mensagem", "Pedido criado com sucesso"
            );
        });
    }

    public Map<String, Object> processOrder(long orderId) {
        if (pendingOrders.get() > 0) {
            pendingOrders.decrementAndGet();
            ordersByStatus.get("PENDING").decrementAndGet();
            ordersByStatus.get("COMPLETED").incrementAndGet();
        }
        return Map.of(
                "orderId", orderId,
                "status", "COMPLETED",
                "mensagem", "Pedido processado com sucesso"
        );
    }

    public Map<String, Object> cancelOrder(long orderId) {
        ordersCancelledCounter.increment();
        if (pendingOrders.get() > 0) {
            pendingOrders.decrementAndGet();
            ordersByStatus.get("PENDING").decrementAndGet();
        }
        ordersByStatus.get("CANCELLED").incrementAndGet();
        return Map.of(
                "orderId", orderId,
                "status", "CANCELLED",
                "mensagem", "Pedido cancelado"
        );
    }

    public Map<String, Object> getStats() {
        return Map.of(
                "pendentes", pendingOrders.get(),
                "receitaTotalReais", totalRevenue.get() / 100.0,
                "porStatus", ordersByStatus.entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().get()
                        ))
        );
    }

}
