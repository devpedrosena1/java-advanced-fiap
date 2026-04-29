package br.com.fiap.javaadv.observability.actuator;


import br.com.fiap.javaadv.observability.service.OrderService;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Endpoint(id = "orders")
public class OrderActuatorEndpoint {

    private final OrderService orderService;

    public OrderActuatorEndpoint(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * GET /actuator/orders
     * Retorna o resumo operacional dos pedidos em tempo real.
     */
    @ReadOperation
    public Map<String, Object> orderSummary() {
        Map<String, Object> stats = orderService.getStats();
        return Map.of(
                "descricao", "Resumo operacional de pedidos",
                "status", "UP",
                "dados", stats
        );
    }

    /**
     * POST /actuator/orders
     * Cria um pedido de teste diretamente pelo actuator (útil para testes de carga).
     */
    @WriteOperation
    public Map<String, Object> createTestOrder() {
        Map<String, Object> order = orderService.createOrder();
        return Map.of(
                "mensagem", "Pedido de teste criado via Actuator",
                "pedido", order
        );
    }

}
