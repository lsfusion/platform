package lsfusion.server.data.query;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.classes.NumericClass;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.Type;

public interface TypeEnvironment {
    void addNeedRecursion(ImList<Type> types); // assert что все типы уже есть

    void addNeedType(ConcatenateType concType);

    void addNeedSafeCast(Type type);

    void addNeedAggOrder(GroupType groupType, ImList<Type> types);
}
