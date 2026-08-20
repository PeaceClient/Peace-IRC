package com.peace.test;

@FunctionalInterface
public interface ThrowableConsumer<T, E extends Exception> {
    void accept(T consumable) throws E;
}
