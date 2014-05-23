package lsfusion.server.data;

import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.Type;

import java.sql.SQLException;

public interface TypePool {

    void ensureRecursion(ImList<Type> types) throws SQLException;
    void ensureConcType(ConcatenateType concType) throws SQLException;
    
    void ensureSafeCast(Type type) throws SQLException;

    void ensureGroupAggOrder(Pair<GroupType, ImList<Type>> groupAggOrder) throws SQLException;
}
