package com.repocket.androidsdk.shared.eventHandling;

import java.util.ArrayList;
import java.util.List;

public class MyEvent implements MyEventHandler {
    private List<MyEventListener> listeners = new ArrayList<>();

    public MyEventListener addListener(MyEventListener listener) {
        listeners.add(listener);
        return listener;
    }

    public void removeListener(MyEventListener listener) {
        listeners.remove(listener);
    }

    public void removeAllListeners(){
        int size = listeners.size();
        for (int i = 0; i < size; i++) {
            listeners.remove(i);
        }
    }

    public void invoke() {
        for (MyEventListener listener : listeners) {
            listener.notify();
//            listener.handleEvent();
        }
    }
}

