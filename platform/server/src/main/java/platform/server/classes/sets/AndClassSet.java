package platform.server.classes.sets;

import platform.server.data.type.Type;

// по сути на Or
public interface AndClassSet {

    AndClassSet and(AndClassSet node);

    boolean isEmpty();

    boolean containsAll(AndClassSet node);

    OrClassSet getOr();

    Type getType();
}
