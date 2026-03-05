package org.akazukin.loader.api.context.dependency;

public interface IDependencyNode extends INode {
    IDependencyNode[] EMPTY_ARR = new IDependencyNode[0];

    boolean isRequired();
}
