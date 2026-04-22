package com.weather;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WeatherMain — orchestrates the High-Frequency Data Ingestion System.
 *
 * Architecture (Producer-Consumer):
 *
 *   [Station-01] ─┐
 *   [Station-02] ─┤
 *        ...       ├──► WeatherBuffer (ReentrantLock) ──► AlertConsumer ──► SQLite DB
 *   [Station-14] ─┤
 *   [Station-15] ─┘
 *
 * Thread Management:
 *   - A fixed thread pool of 15 threads is used for producers.
 *   - A separate single-thread executor runs the consumer.
 *   - After all producers complete, the AtomicBoolean flag is flipped to
 *     signal the consumer to drain remaining items and exit gracefully.
 */
public class WeatherMain {

    private static final int NUM_STATIONS = 15;

    public static void main(String[] args) throws InterruptedException {

        // 1. Initialise the SQLite database (creates table if absent)
        WeatherDatabase.initialise();

        // 2. Shared buffer and shutdown flag
        WeatherBuffer  buffer  = new WeatherBuffer();
        AtomicBoolean  running = new AtomicBoolean(true);

        // 3. Start the consumer in its own single-thread pool
        ExecutorService consumerPool = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "AlertConsumer-Thread");
            t.setDaemon(false);
            return t;
        });
        consumerPool.submit(new AlertConsumer(buffer, running));

        // 4. Start 15 producer threads (one per weather station)
        ExecutorService producerPool = Executors.newFixedThreadPool(NUM_STATIONS, r -> {
            Thread t = new Thread(r);
            t.setDaemon(false);
            return t;
        });

        for (int id = 1; id <= NUM_STATIONS; id++) {
            producerPool.submit(new WeatherStation(id, buffer));
        }

        // 5. Wait for all producers to finish
        producerPool.shutdown();
        boolean producersDone = producerPool.awaitTermination(5, TimeUnit.MINUTES);

        if (producersDone) {
            System.out.println("[MAIN] All 15 weather stations have finished producing.");
        } else {
            System.out.println("[MAIN] Timeout reached — forcing producer shutdown.");
            producerPool.shutdownNow();
        }

        // 6. Signal consumer to stop after draining the buffer,
        //    then wait for it to finish.
        running.set(false);
        consumerPool.shutdown();
        consumerPool.awaitTermination(30, TimeUnit.SECONDS);

        System.out.println("[MAIN] System shutdown complete. Check weather_alerts.db for records.");
    }
}
