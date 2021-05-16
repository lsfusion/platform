package lsfusion.server.data.type.reader;

import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import org.postgresql.util.PGobject;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PGObjectReader<T> extends AbstractReader<String> {

    public static final PGObjectReader instance = new PGObjectReader();

    @Override
    public String read(ResultSet set, SQLSyntax syntax, String name) throws SQLException {
        Object value = set.getObject(name);
        if (value == null) return null;
        if (value instanceof PGobject) {
            return ((PGobject) value).getValue();
        } else throw new UnsupportedOperationException();
    }

    @Override
    public ExtInt getCharLength() {
        return new ExtInt(100);
    }
}