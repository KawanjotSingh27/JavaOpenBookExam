package com.weather;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AlertConsumer — the single consumer thread in the producer-consumer pipeline.
 *
 * Continuously drains readings from the shared WeatherBuffer.
 * Any reading above the ALERT_THRESHOLD (45°C) is persisted as an
 * Extreme Weather Alert in the SQLite database via WeatherDatabase.saveAlert().
 *
 * Shutdown is controlled by an AtomicBoolean flag set by the main thread after
 * all producers have finished, preventing data loss caused by premature exit.
 */
public class AlertConsumer implements Runnable {

    private static final double ALERT_THRESHOLD = 45.0;

    private final WeatherBuffer   buffer;
    private final AtomicBoolean   running;

    /**
     * @param buffer  Shared thread-safe buffer to drain readings from.
     * @param running Flag; when set to false the consumer finishes its current
     *                read and then exits cleanly.
     */
    public AlertConsumer(WeatherBuffer buffer, AtomicBoolean running) {
        this.buffer  = buffer;
        this.running = running;
    }

    @Override
    public void run() {
        System.out.println("[CONSUMER] Alert consumer started. Threshold = 45°C");

        while (running.get()) {
            try {
                double temperature = buffer.consume();

                if (temperature > ALERT_THRESHOLD) {
                    System.out.printf("[CONSUMER] ⚠ Extreme temp detected: %.2f°C → persisting alert.%n",
                            temperature);
                    WeatherDatabase.saveAlert(temperature);
                } else {
                    System.out.printf("[CONSUMER] Normal reading: %.2f°C — no action.%n", temperature);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("[CONSUMER] Interrupted. Exiting.");
                return;
            }
        }

        System.out.println("[CONSUMER] All producers finished. Consumer shutting down.");
    }
}
