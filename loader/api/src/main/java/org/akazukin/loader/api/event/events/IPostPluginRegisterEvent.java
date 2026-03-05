package org.akazukin.loader.api.event.events;

import org.akazukin.loader.api.IPluginContext;

public interface IPostPluginRegisterEvent extends IPluginLifecycleEvent {
    IPluginContext getPluginContext();
}
