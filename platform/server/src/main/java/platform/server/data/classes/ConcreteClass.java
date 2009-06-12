package platform.server.data.classes;

import platform.server.data.classes.where.ClassSet;

public interface ConcreteClass extends RemoteClass,ClassSet {

    boolean inSet(ClassSet set);
    
}
