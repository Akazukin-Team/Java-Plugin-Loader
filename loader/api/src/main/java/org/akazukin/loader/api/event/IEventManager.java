package org.akazukin.loader.api.event;

import org.akazukin.event.IListenable;
import org.akazukin.event.method.IEventMethod;
import org.akazukin.event.target.IEventCondition;

public interface IEventManager {
    void registerListeners(final IListenable... listeners);

    void registerListener(final IListenable listener);

    <U> void registerListener(final IEventMethod<U> method, final IEventCondition condition);

    void unregisterListener(final IListenable IListenable);
}
