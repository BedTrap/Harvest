package me.eureka.harvest.systems.modules.client.Rotations;

public interface Producer<T> {
    T create();
}