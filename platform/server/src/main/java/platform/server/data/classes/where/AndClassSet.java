package platform.server.data.classes.where;

import platform.server.data.types.Type;

// по сути на Or
public interface AndClassSet {

    AndClassSet and(AndClassSet node);

    boolean isEmpty();

    boolean containsAll(AndClassSet node);

    OrClassSet getOr();

    Type getType();
}
