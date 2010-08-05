package platform.server.classes.sets;

import platform.server.data.type.Type;

// по сути на Or
public interface AndClassSet {

    AndClassSet getKeepClass();

    AndClassSet and(AndClassSet node);

    // если не or'ся возвращаем null
    AndClassSet or(AndClassSet node);

    boolean isEmpty();

    boolean containsAll(AndClassSet node);

    OrClassSet getOr();

    Type getType();
}
