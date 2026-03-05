package org.akazukin.loader.api.event.events;

import org.akazukin.event.event.IEvent;

public interface IPluginLifecycleEvent extends IEvent {
    String getPluginId();
}
