package lsfusion.server.data.type;

import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.data.ParsedStatement;

import java.sql.Connection;
import java.sql.SQLException;

public class ParsedParamString extends ParsedString {

    private final ImList<String> params;

    public ParsedParamString(String string, ImList<String> params) {
        super(string);
        this.params = params;
    }

    @Override
    protected boolean calcTwins(TwinImmutableObject o) {
        return super.calcTwins(o) && params.equals(((ParsedParamString)o).params);
    }

    @Override
    public int immutableHashCode() {
        return super.immutableHashCode() * 31 + params.hashCode();
    }

    @Override
    public void fillEnv(MList<String> mPreparedParams) {
        super.fillEnv(mPreparedParams);
        mPreparedParams.addAll(params);
    }

    public ParsedStatement prepareStatement(Connection connection) throws SQLException {
        return new ParsedStatement(connection.prepareStatement(string), params);
    }
}
