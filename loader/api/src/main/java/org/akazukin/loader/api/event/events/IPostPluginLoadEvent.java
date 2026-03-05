package org.akazukin.loader.api.event.events;

import org.akazukin.loader.api.IPluginContext;

public interface IPostPluginLoadEvent extends IPluginLifecycleEvent {
    IPluginContext getPluginContext();
}
