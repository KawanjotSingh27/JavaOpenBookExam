package com.weather;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * WeatherStation — simulates a single physical weather sensor.
 *
 * Each station runs as an independent thread and pushes randomly generated
 * temperature readings into the shared WeatherBuffer at random intervals
 * (between 200 ms and 1000 ms) to mimic real-world sensor jitter.
 *
 * Temperature range: −10°C to 55°C (includes extreme values > 45°C to trigger alerts).
 */
public class WeatherStation implements Runnable {

    private final int stationId;
    private final WeatherBuffer buffer;
    private final Random random = new Random();

    // Number of readings each station produces before shutting down.
    private static final int TOTAL_READINGS = 20;

    /**
     * @param stationId Unique identifier (1–15) for this weather station.
     * @param buffer    Shared thread-safe buffer to push readings into.
     */
    public WeatherStation(int stationId, WeatherBuffer buffer) {
        this.stationId = stationId;
        this.buffer    = buffer;
    }

    @Override
    public void run() {
        for (int i = 0; i < TOTAL_READINGS; i++) {
            // Generate a temperature between -10°C and 55°C
            double temperature = -10 + (random.nextDouble() * 65);

            try {
                buffer.produce(temperature);
                // Simulate varying sensor sampling rates
                long delay = 200 + random.nextInt(800);
                TimeUnit.MILLISECONDS.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.printf("[Station-%02d] Interrupted. Shutting down.%n", stationId);
                return;
            }
        }
        System.out.printf("[Station-%02d] Finished producing all readings.%n", stationId);
    }
}
