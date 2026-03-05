package org.akazukin.loader.api.event.events;

import org.akazukin.loader.api.IPluginContext;

public interface IPostPluginUnloadEvent extends IPluginLifecycleEvent {
    IPluginContext getPluginContext();
}
