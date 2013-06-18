package lsfusion.server.classes.sets;

import lsfusion.server.classes.ValueClass;

public interface OrClassSet {

    OrClassSet or(OrClassSet node);
    boolean containsAll(OrClassSet node, boolean implicitCast);

    OrClassSet and(OrClassSet node);

    ValueClass getCommonClass();
}
