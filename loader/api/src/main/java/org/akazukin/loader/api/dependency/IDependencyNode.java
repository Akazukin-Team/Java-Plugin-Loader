package org.akazukin.loader.api.dependency;

public interface IDependencyNode extends INode {
    IDependencyNode[] EMPTY_ARR = new IDependencyNode[0];

    boolean isRequired();
}
