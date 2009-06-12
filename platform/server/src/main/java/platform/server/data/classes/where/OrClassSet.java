package platform.server.data.classes.where;

import platform.server.data.classes.ValueClass;

public interface OrClassSet {

    OrClassSet or(OrClassSet node);
    boolean containsAll(OrClassSet node);

    ValueClass getCommonClass();
}
