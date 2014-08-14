package lsfusion.server.classes.sets;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.type.Type;

// множество классов, от AndClassSet и OrClassSet, тем что abstract'ы не считаются конкретными, нет множества конкретных классов и т.п.
// используется в resolve'инге и выводе типов
public interface ResolveClassSet {
    
    boolean containsAll(ResolveClassSet set, boolean implicitCast);
    
    ResolveClassSet and(ResolveClassSet set);

    ResolveClassSet or(ResolveClassSet set);

    boolean isEmpty();
    
    Type getType();

    ValueClass getCommonClass();
    
    AndClassSet toAnd();

    String getCanonicalName();
}
