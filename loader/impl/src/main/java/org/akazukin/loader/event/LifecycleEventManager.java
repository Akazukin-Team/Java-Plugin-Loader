package org.akazukin.loader.event;

import org.akazukin.event.EventManager;
import org.akazukin.loader.api.event.IEventManager;
import org.akazukin.loader.api.event.events.IPluginLifecycleEvent;

public class LifecycleEventManager extends EventManager<IPluginLifecycleEvent> implements IEventManager {
    public LifecycleEventManager() {
        super(IPluginLifecycleEvent.class);
    }
}
