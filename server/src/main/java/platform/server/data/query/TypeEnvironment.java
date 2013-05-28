package platform.server.data.query;

import platform.base.col.interfaces.immutable.ImList;
import platform.server.data.type.Type;

public interface TypeEnvironment {
    void addNeedRecursion(ImList<Type> types); // assert что все типы уже есть

    void addNeedType(ImList<Type> types);
}
