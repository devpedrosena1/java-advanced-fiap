# Observability Demo — Spring Boot + Prometheus + Grafana

Projeto de exemplo demonstrando observabilidade completa com Spring Boot 3, Micrometer, Prometheus e Grafana, incluindo um **endpoint customizado do Spring Actuator** exposto ao Prometheus.

---

## Estrutura do Projeto

```
observability-demo/
├── src/main/java/com/example/observability/
│   ├── ObservabilityDemoApplication.java
│   ├── actuator/
│   │   └── OrdersActuatorEndpoint.java   ← Actuator customizado
│   ├── controller/
│   │   └── OrderController.java
│   └── service/
│       └── OrderService.java             ← Métricas registradas aqui
├── src/main/resources/
│   └── application.yml
├── monitoring/
│   ├── prometheus.yml
│   └── grafana/
│       ├── provisioning/
│       │   ├── datasources/prometheus.yml
│       │   └── dashboards/dashboards.yml
│       └── dashboards/
│           └── observability-demo.json   ← Dashboard pré-configurado
├── docker-compose.yml
├── Dockerfile
└── pom.xml
```

---

## Como Executar

### Pré-requisitos
- Docker + Docker Compose
- Java 21 + Maven (se quiser rodar localmente sem Docker)

### Subir com Docker Compose

```bash
docker compose up --build
```

> Com Colima: certifique-se que está rodando com `colima start`

### Acessos

| Serviço    | URL                              | Credenciais     |
|------------|----------------------------------|-----------------|
| Aplicação  | http://localhost:8080            | —               |
| Actuator   | http://localhost:8080/actuator   | —               |
| Prometheus | http://localhost:9090            | —               |
| Grafana    | http://localhost:3000            | admin / admin   |

---

## Endpoint Customizado do Actuator

O endpoint `orders` foi criado com `@Endpoint(id = "orders")` e expõe dados operacionais de pedidos diretamente no Actuator.

```bash
# Resumo dos pedidos
GET http://localhost:8080/actuator/orders

# Criar pedido de teste via Actuator
POST http://localhost:8080/actuator/orders
```

As métricas registradas via `MeterRegistry` no `OrderService` são automaticamente coletadas pelo Prometheus em `/actuator/prometheus`.

---

## API REST de Pedidos

```bash
# Criar pedido (gera métricas de latência e contagem)
curl -X POST http://localhost:8080/api/orders

# Processar pedido
curl -X PUT http://localhost:8080/api/orders/{id}/process

# Cancelar pedido
curl -X DELETE http://localhost:8080/api/orders/{id}

# Ver estatísticas
curl http://localhost:8080/api/orders/stats
```

---

## Métricas Customizadas Expostas

| Métrica                               | Tipo    | Descrição                        |
|---------------------------------------|---------|----------------------------------|
| `orders_created_total`                | Counter | Total de pedidos criados         |
| `orders_cancelled_total`              | Counter | Total de pedidos cancelados      |
| `orders_pending_current`              | Gauge   | Pedidos pendentes no momento     |
| `orders_revenue_total`                | Gauge   | Receita acumulada em R$          |
| `orders_by_status{status=...}`        | Gauge   | Pedidos agrupados por status     |
| `orders_processing_duration_seconds`  | Timer   | Latência de processamento (p50/p95/p99) |

---

## Dashboard Grafana

O dashboard **"Observability Demo - Spring Boot"** é provisionado automaticamente na pasta `Spring Boot` do Grafana com os seguintes painéis:

- Contadores de pedidos criados, cancelados, pendentes e receita
- Taxa de criação de pedidos (req/s)
- Latência de processamento (p50, p95, p99)
- Distribuição de pedidos por status (donut)
- Latência HTTP por endpoint (p95)
- Uso de memória Heap da JVM
- Threads ativas da JVM

---

## Rodar Localmente (sem Docker)

```bash
mvn spring-boot:run
```

> Neste caso, o Prometheus e o Grafana precisam ser iniciados separadamente apontando para `localhost:8080`.
