package com.weather;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * WeatherBuffer — a bounded, thread-safe queue shared between
 * 15 Producer (WeatherStation) threads and one Consumer (AlertConsumer) thread.
 *
 * Design rationale:
 *  - ReentrantLock + Condition variables are used instead of raw synchronized
 *    blocks because they allow separate "notFull" and "notEmpty" conditions,
 *    which avoids unnecessary wake-ups and reduces contention overhead.
 *  - The buffer capacity (CAPACITY) is deliberately finite so that fast
 *    producers do not unboundedly consume heap memory (green-computing principle).
 */
public class WeatherBuffer {

    private static final int CAPACITY = 50;

    private final Queue<Double> buffer = new LinkedList<>();
    private final ReentrantLock lock = new ReentrantLock(true); // fair lock
    private final Condition notFull  = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    /**
     * Called by a WeatherStation (producer).
     * Blocks if the buffer is full until space becomes available.
     *
     * @param temperature Reading produced by a weather station (°C).
     * @throws InterruptedException if the thread is interrupted while waiting.
     */
    public void produce(double temperature) throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (buffer.size() == CAPACITY) {
                notFull.await();          // release lock and wait
            }
            buffer.offer(temperature);
            System.out.printf("[PRODUCER] Buffered %.2f°C  (buffer size: %d)%n",
                    temperature, buffer.size());
            notEmpty.signal();            // wake one consumer
        } finally {
            lock.unlock();
        }
    }

    /**
     * Called by the AlertConsumer (consumer).
     * Blocks if the buffer is empty until data arrives.
     *
     * @return The oldest temperature reading in the buffer.
     * @throws InterruptedException if the thread is interrupted while waiting.
     */
    public double consume() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (buffer.isEmpty()) {
                notEmpty.await();         // release lock and wait
            }
            double temperature = buffer.poll();
            notFull.signal();             // wake one producer
            return temperature;
        } finally {
            lock.unlock();
        }
    }
}
