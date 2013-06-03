package lsfusion.server.data;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.Type;

import java.sql.SQLException;

public interface TypePool {

    void ensureRecursion(ImList<Type> types) throws SQLException;
    void ensureConcType(ConcatenateType concType) throws SQLException;
}
