package lsfusion.server.data.sql.lambda;

import lsfusion.base.lambda.E2Callable;
import lsfusion.server.data.sql.exception.SQLHandledException;

import java.sql.SQLException;

public interface SQLCallable<R> extends E2Callable<R, SQLException, SQLHandledException> {
}
