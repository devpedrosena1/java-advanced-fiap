package br.com.fiap.javaadv.observability.config;

import br.com.fiap.javaadv.observability.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DataLoader — popula a aplicação com centenas de milhares de eventos de pedido
 * ao iniciar, usando múltiplas threads para simular carga realista.
 *
 * Configurações:
 *   TOTAL_ORDERS      → quantidade total de pedidos a criar
 *   THREAD_COUNT      → paralelismo (threads simultâneas)
 *   CANCEL_RATE       → fração dos pedidos que serão cancelados (0.0 a 1.0)
 *   PROCESS_RATE      → fração dos pedidos que serão processados (0.0 a 1.0)
 *   LOG_INTERVAL      → a cada quantos pedidos logar progresso
 */
@Component
public class DataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private static final int  TOTAL_ORDERS  = 200_000;
    private static final int  THREAD_COUNT  = 16;
    private static final double CANCEL_RATE = 0.10; // 10% cancelados
    private static final double PROCESS_RATE = 0.75; // 75% processados
    private static final int  LOG_INTERVAL  = 10_000;

    private final OrderService orderService;
    private final Random random = new Random();

    public DataLoader(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("--------------------------DATAALOADER INICIADO---------------------------");
        log.info("=== DataLoader iniciado: {} pedidos em {} threads ===", TOTAL_ORDERS, THREAD_COUNT);
        long start = System.currentTimeMillis();

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        AtomicInteger created   = new AtomicInteger(0);
        AtomicInteger processed = new AtomicInteger(0);
        AtomicInteger cancelled = new AtomicInteger(0);

        int batchSize = TOTAL_ORDERS / THREAD_COUNT;
        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < THREAD_COUNT; t++) {
            int count = (t == THREAD_COUNT - 1)
                    ? TOTAL_ORDERS - (batchSize * (THREAD_COUNT - 1))  // última thread pega o resto
                    : batchSize;

            futures.add(executor.submit(() -> {
                for (int i = 0; i < count; i++) {
                    try {
                        Map<String, Object> order = orderService.createOrder();
                        long orderId = (long) order.get("orderId");
                        int total = created.incrementAndGet();

                        double roll = random.nextDouble();

                        if (roll < CANCEL_RATE) {
                            orderService.cancelOrder(orderId);
                            cancelled.incrementAndGet();
                        } else if (roll < CANCEL_RATE + PROCESS_RATE) {
                            orderService.processOrder(orderId);
                            processed.incrementAndGet();
                        }
                        // o restante fica PENDING — contribui para o gauge orders_pending_current

                        if (total % LOG_INTERVAL == 0) {
                            log.info("Progresso: {}/{} pedidos | processados={} cancelados={}",
                                    total, TOTAL_ORDERS, processed.get(), cancelled.get());
                        }

                    } catch (Exception e) {
                        log.warn("Erro ao criar pedido: {}", e.getMessage());
                    }
                }
            }));
        }

        // Aguarda todas as threads
        for (Future<?> f : futures) {
            f.get();
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);

        long elapsed = System.currentTimeMillis() - start;
        log.info("=== DataLoader concluído em {}s ===", elapsed / 1000);
        log.info("  Total criados  : {}", created.get());
        log.info("  Processados    : {}", processed.get());
        log.info("  Cancelados     : {}", cancelled.get());
        log.info("  Pendentes      : {}", created.get() - processed.get() - cancelled.get());
        log.info("  Throughput     : {} pedidos/s", created.get() / Math.max(1, elapsed / 1000));
    }
}
