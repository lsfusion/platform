package platform.server.classes.sets;

import platform.server.classes.ValueClass;
import platform.server.classes.ConcreteObjectClass;
import platform.server.classes.BaseClass;

public interface OrClassSet {

    OrClassSet or(OrClassSet node);
    boolean containsAll(OrClassSet node);

    ValueClass getCommonClass();
}
