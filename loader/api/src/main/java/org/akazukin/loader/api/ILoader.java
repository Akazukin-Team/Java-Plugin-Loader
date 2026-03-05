package org.akazukin.loader.api;

import org.akazukin.loader.api.event.IEventManager;
import org.akazukin.loader.api.manager.IPluginLoader;
import org.akazukin.loader.api.manager.IPluginManager;
import org.akazukin.loader.api.manager.IPluginResolver;

public interface ILoader {
    IEventManager getEventMgr();

    IPluginManager getPluginMgr();

    IPluginResolver getPluginResolver();

    IPluginLoader getLoader();
}
