package org.akazukin.loader.api.event.events;

import org.akazukin.event.event.IEvent;

public interface IBatchPluginLifecycleEvent extends IEvent {
    String[] getPluginIds();
}
