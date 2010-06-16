package platform.server.classes.sets;

import platform.server.classes.ValueClass;

public interface OrClassSet {

    OrClassSet or(OrClassSet node);
    boolean containsAll(OrClassSet node);

    OrClassSet and(OrClassSet node);

    ValueClass getCommonClass();
}
