package lsfusion.server.classes.sets;

import lsfusion.server.classes.ValueClass;

public interface OrClassSet {

    OrClassSet or(OrClassSet node);
    boolean containsAll(OrClassSet node, boolean implicitCast);
    
    boolean isEmpty();

    OrClassSet and(OrClassSet node);

    ValueClass getCommonClass();
    AndClassSet getCommonAnd();
}
