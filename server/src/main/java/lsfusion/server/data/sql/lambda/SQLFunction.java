package lsfusion.server.data.sql.lambda;

import lsfusion.base.col.interfaces.mutable.mapvalue.ThrowingFunction;
import lsfusion.server.data.sql.exception.SQLHandledException;

import java.sql.SQLException;

@FunctionalInterface
public interface SQLFunction<V, M> extends ThrowingFunction<V, M, SQLException, SQLHandledException> {
}
