package lsfusion.server.data.type.reader;

import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.exec.TypeEnvironment;

import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class AbstractReader<T> implements Reader<T> {

    public String writeDeconc(SQLSyntax syntax, TypeEnvironment env) {
        return "?";
    }

    public void readDeconc(String source, String name, MExclMap<String, String> mResult, SQLSyntax syntax, TypeEnvironment typeEnv) {
        mResult.exclAdd(name, source);
    }

    public int getSize(T value) {
        throw new UnsupportedOperationException();
    }
}
