package me.eureka.harvest.systems.utils.other;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Threading {
    public static ExecutorService thread = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * (1 + 13 / 3));
}
