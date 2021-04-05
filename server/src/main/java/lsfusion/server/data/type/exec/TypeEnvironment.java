package lsfusion.server.data.type.exec;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.table.SessionTable;
import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.TypeFunc;
import lsfusion.server.logics.classes.data.ArrayClass;

public interface TypeEnvironment {
    void addNeedRecursion(Object types); // assert что все типы уже есть

    void addNeedType(ConcatenateType concType);

    void addNeedTableType(SessionTable.TypeStruct tableType);

    void addNeedArrayClass(ArrayClass tableType);

    void addNeedSafeCast(Type type, Boolean isInt);

    void addNeedAggOrder(GroupType groupType, ImList<Type> types);

    void addNeedTypeFunc(TypeFunc groupType, Type type);
}
