package lsfusion.server.data.query;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.classes.NumericClass;
import lsfusion.server.data.SessionTable;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.type.ArrayClass;
import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.Type;

public interface TypeEnvironment {
    void addNeedRecursion(Object types); // assert что все типы уже есть

    void addNeedType(ConcatenateType concType);

    void addNeedTableType(SessionTable.TypeStruct tableType);

    void addNeedArrayClass(ArrayClass tableType);

    void addNeedSafeCast(Type type);

    void addNeedAggOrder(GroupType groupType, ImList<Type> types);

    void addNeedTypeFunc(TypeFunc groupType, Type type);
}
