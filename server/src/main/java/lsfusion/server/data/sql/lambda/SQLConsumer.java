package lsfusion.server.data.sql.lambda;

import lsfusion.base.lambda.E2Callable;
import lsfusion.base.lambda.E2Consumer;
import lsfusion.server.data.sql.exception.SQLHandledException;

import java.sql.SQLException;

@FunctionalInterface
public interface SQLConsumer<R> extends E2Consumer<R, SQLException, SQLHandledException> {
}
