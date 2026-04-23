package lsfusion.server.logics.classes.data.file;

import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import org.postgresql.util.PGobject;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public abstract class AJSONClass extends StringFileBasedClass {

    public AJSONClass(LocalizedString caption) {
        super(caption, "json");
    }

    @Override
    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setObject(num, value, Types.OTHER);
    }

    @Override
    public String read(Object value) {
        return value instanceof PGobject ? ((PGobject) value).getValue() : value instanceof String ? (String) value : null;
    }

    @Override
    public String getDefaultValue() {
        return "{}";
    }
}
