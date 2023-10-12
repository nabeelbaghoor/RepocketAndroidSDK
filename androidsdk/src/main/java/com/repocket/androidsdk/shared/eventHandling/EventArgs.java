package com.repocket.androidsdk.shared.eventHandling;

import java.util.EventObject;

public class EventArgs extends EventObject {
    /**
     * Constructs a prototypical Event.
     *
     * @param source the object on which the Event initially occurred
     * @throws IllegalArgumentException if source is null
     */
    public EventArgs(Object source) {
        super(source);
    }
}
