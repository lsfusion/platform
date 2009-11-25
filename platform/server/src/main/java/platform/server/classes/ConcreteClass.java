package platform.server.classes;

import platform.server.classes.sets.AndClassSet;

public interface ConcreteClass extends RemoteClass, AndClassSet {

    boolean inSet(AndClassSet set);
    
}
