package platform.server.data.type;

import platform.base.col.interfaces.mutable.MExclMap;
import platform.base.col.interfaces.mutable.MMap;
import platform.server.data.query.TypeEnvironment;
import platform.server.data.sql.SQLSyntax;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface Reader<T> {
    T read(Object value);

    // блок для случая когда JDBC драйвер не поддерживает STRUCT'ы
    String writeDeconc(SQLSyntax syntax, TypeEnvironment env);

    void readDeconc(String source, String name, MExclMap<String, String> mResult, SQLSyntax syntax, TypeEnvironment typeEnv);

    T read(ResultSet set, SQLSyntax syntax, String name) throws SQLException;
}
