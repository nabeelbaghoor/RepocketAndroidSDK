package com.repocket.androidsdk.shared.eventHandling;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class Event {
    private Set<Consumer<MyEventArgs>> listeners = new HashSet();

    public Consumer<MyEventArgs> addListener(Consumer<MyEventArgs> listener) {
        listeners.add(listener);
        return listener;
    }

    public void broadcast(MyEventArgs args) {
        listeners.forEach(x -> x.accept(args));
    }
}
