package lsfusion.server.data.type.exec;

import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.TypeFunc;
import lsfusion.server.logics.classes.data.ArrayClass;

import java.sql.SQLException;

public interface TypePool {

    void ensureRecursion(Object types) throws SQLException;
    void ensureConcType(ConcatenateType concType) throws SQLException;
    
    void ensureSafeCast(Pair<Type, Boolean> type) throws SQLException;

    void ensureGroupAggOrder(Pair<GroupType, ImList<Type>> groupAggOrder);

    void ensureTypeFunc(Pair<TypeFunc, Type> tf);
    
    void ensureArrayClass(ArrayClass arrayClass);
}
