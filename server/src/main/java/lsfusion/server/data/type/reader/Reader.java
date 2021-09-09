package lsfusion.server.data.type.reader;

import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.exec.TypeEnvironment;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface Reader<T> {
    ExtInt getCharLength();
    int getSize(T value);

    // блок для случая когда JDBC драйвер не поддерживает STRUCT'ы
    String writeDeconc(SQLSyntax syntax, TypeEnvironment env);

    void readDeconc(String source, String name, MExclMap<String, String> mResult, SQLSyntax syntax, TypeEnvironment typeEnv);

    T read(ResultSet set, SQLSyntax syntax, String name) throws SQLException;
}
