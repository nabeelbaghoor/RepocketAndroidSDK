package com.repocket.androidsdk.shared.eventHandling;

import java.util.EventListener;

public interface MyEventHandler extends EventListener {
    MyEventListener addListener(MyEventListener listener);
    void removeListener(MyEventListener listener);
    void invoke();
}

