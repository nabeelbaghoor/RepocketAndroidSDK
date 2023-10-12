package com.repocket.androidsdk.shared;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class EventHandler<T> {
    private final Set<Consumer<T>> listeners = new HashSet();

    public Consumer<T> addListener(Consumer<T> listener) {
        listeners.add(listener);
        return listener;
    }

    public void broadcast(T args) {
        listeners.forEach(x -> x.accept(args));
    }

    public void removeAllListeners() {
        listeners.clear();
    }
}
