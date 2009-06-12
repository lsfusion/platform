package platform.server.data.classes.where;

import platform.server.data.classes.ConcreteValueClass;
import platform.server.data.classes.ValueClass;
import platform.server.data.types.Type;

import java.util.Random;

// по сути на Or
public interface ClassSet {

    ClassSet and(ClassSet node);

    boolean isEmpty();

    boolean containsAll(ClassSet node);

    OrClassSet getOr();

    Type getType();
}
