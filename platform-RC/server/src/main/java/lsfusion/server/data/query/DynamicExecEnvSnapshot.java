package lsfusion.server.data.query;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.*;
import lsfusion.server.data.type.ParseInterface;

import java.sql.SQLException;
import java.sql.Statement;

// ThreadSafe
public interface DynamicExecEnvSnapshot<OE, S extends DynamicExecEnvSnapshot<OE, S>> extends DynamicExecEnvOuter<OE, S> {

    void beforeOuter(SQLCommand command, SQLSession session, ImMap<String, ParseInterface> paramObjects, OperationOwner owner, PureTimeInterface runTime) throws SQLException, SQLHandledException;

    void afterOuter(SQLSession session, OperationOwner owner) throws SQLException;

    // после readLock сессии, но до получения connection'а
    void beforeConnection(SQLSession session, OperationOwner owner) throws SQLException;

    void afterConnection(SQLSession session, OperationOwner owner) throws SQLException;

    void beforeStatement(SQLSession sqlSession, ExConnection connection, String command, OperationOwner owner) throws SQLException;

    void afterStatement(SQLSession sqlSession, ExConnection connection, String command, OperationOwner owner) throws SQLException;

    void beforeExec(Statement statement, SQLSession session) throws SQLException;

    boolean hasRepeatCommand();

    boolean isTransactTimeout();

    boolean needConnectionLock();

    S forAnalyze(); // важно чтобы не было repeatCommand, потому как иначе может заполниться handler, а пойдет repeat и он второй раз будет выполняться

    ImMap<SQLQuery, MaterializedQuery> getMaterializedQueries();
}
