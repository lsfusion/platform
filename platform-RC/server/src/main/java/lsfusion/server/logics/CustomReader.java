package lsfusion.server.logics;

import lsfusion.base.ExtInt;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.Reader;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CustomReader<T> implements Reader<Integer> {

    @Override
    public Integer read(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ExtInt getCharLength() {
       return new ExtInt(30);
    }

    @Override
    public int getSize(Integer value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String writeDeconc(SQLSyntax syntax, TypeEnvironment env) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void readDeconc(String source, String name, MExclMap<String, String> mResult, SQLSyntax syntax, TypeEnvironment typeEnv) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Integer read(ResultSet set, SQLSyntax syntax, String name) throws SQLException {
        return set.getRow();
    }


}
