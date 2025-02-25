package com.chancetop.naixt.agent.service;

/**
 * @author stephen
 */
public class NaixtService {
    public void stop() {
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.exit(0);
        }).start();
    }
}
