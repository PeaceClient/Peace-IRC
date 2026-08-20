package com.peace.test;

@FunctionalInterface
public interface ThrowableFunction<T, R, E extends Exception> {
    R apply(T consumable) throws E;
}
