package lsfusion.server.data.query;

import lsfusion.server.data.*;

import java.sql.Connection;
import java.sql.SQLException;

public interface StaticExecuteEnvironment {

    void before(SQLSession sqlSession, ExConnection connection, String command, OperationOwner owner) throws SQLException;

    void after(SQLSession sqlSession, ExConnection connection, String command, OperationOwner owner) throws SQLException;

    Object before(Connection connection, TypePool typePool, String command, OperationOwner owner) throws SQLException;

    void after(Connection connection, TypePool typePool, String command, OperationOwner owner, Object prevEnvState) throws SQLException;

    boolean hasRecursion();

    boolean hasNotMaterializable();

    boolean isNoPrepare();

    EnsureTypeEnvironment getEnsureTypes();
}
