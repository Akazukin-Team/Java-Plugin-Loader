package org.akazukin.loader.api;

import org.akazukin.loader.api.event.IEventManager;
import org.akazukin.loader.api.manager.IPluginLoader;
import org.akazukin.loader.api.manager.IPluginManager;
import org.akazukin.loader.api.manager.IPluginResolver;

import java.io.Closeable;

public interface ILoader extends Closeable {
    IEventManager getEventMgr();

    IPluginManager getPluginMgr();

    IPluginResolver getPluginResolver();

    IPluginLoader getLoader();
}
