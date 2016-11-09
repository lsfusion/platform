package lsfusion.server.data.query;

import lsfusion.base.Pair;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.Settings;
import lsfusion.server.caches.AbstractTranslateValues;
import lsfusion.server.caches.TranslateValues;
import lsfusion.server.data.*;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.translator.MapValuesTranslate;
import lsfusion.server.data.type.ArrayClass;
import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.Type;

import java.sql.Connection;
import java.sql.SQLException;

public interface StaticExecuteEnvironment {

    void before(SQLSession sqlSession, ExConnection connection, String command, OperationOwner owner) throws SQLException;

    void after(SQLSession sqlSession, ExConnection connection, String command, OperationOwner owner) throws SQLException;

    void before(Connection connection, TypePool typePool, String command, OperationOwner owner) throws SQLException;

    void after(Connection connection, TypePool typePool, String command, OperationOwner owner) throws SQLException;

    boolean hasRecursion();

    boolean isNoPrepare();

    EnsureTypeEnvironment getEnsureTypes();
}
