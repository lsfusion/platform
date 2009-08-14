package platform.server.data.classes;

import platform.server.data.classes.where.AndClassSet;

public interface ConcreteClass extends RemoteClass, AndClassSet {

    boolean inSet(AndClassSet set);
    
}
