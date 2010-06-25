package platform.server.classes.sets;

import platform.server.data.type.Type;
import platform.server.classes.ValueClass;

// по сути на Or
public interface AndClassSet {

    ValueClass getBaseClass();

    AndClassSet and(AndClassSet node);

    // если не or'ся возвращаем null
    AndClassSet or(AndClassSet node);

    boolean isEmpty();

    boolean containsAll(AndClassSet node);

    OrClassSet getOr();

    Type getType();
}
