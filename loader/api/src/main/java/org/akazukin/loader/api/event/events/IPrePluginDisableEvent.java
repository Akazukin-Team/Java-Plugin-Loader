package org.akazukin.loader.api.event.events;

import org.akazukin.event.event.ICancellableEvent;
import org.akazukin.loader.api.IPluginContext;

public interface IPrePluginDisableEvent extends IPluginLifecycleEvent, ICancellableEvent {
    IPluginContext getPluginContext();
}
