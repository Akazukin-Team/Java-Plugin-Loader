package org.akazukin.loader.api.event.events;

import org.akazukin.event.event.ICancellableEvent;
import org.akazukin.loader.api.context.IPluginContext;

public interface IPrePluginUnregisterEvent extends IPluginLifecycleEvent, ICancellableEvent {
    IPluginContext getPluginContext();
}
