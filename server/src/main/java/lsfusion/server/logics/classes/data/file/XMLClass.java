package lsfusion.server.logics.classes.data.file;

import com.google.common.base.Throwables;
import lsfusion.interop.classes.DataType;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import org.postgresql.jdbc.PgSQLXML;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class XMLClass extends StringFileBasedClass {

    public final static XMLClass instance = new XMLClass();

    public XMLClass() {
        super(LocalizedString.create("{classes.xml}"), "xml");
    }

    @Override
    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setString(num, (String) value);
    }

    static {
        DataClass.storeClass(instance);
    }

    @Override
    public String getDBString(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return syntax.getXML();
    }

    @Override
    public String getSID() {
        return "XML";
    }

    @Override
    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof XMLClass ? this : null;
    }

    @Override
    public byte getTypeID() {
        return DataType.XML;
    }

    @Override
    public String getCastFromStatic(String value) {
        return "cast_static_file_to_xml(" + value + ")";
    }

    @Override
    public String getCastToStatic(String value) {
        return "cast_xml_to_static_file(" + value + ")";
    }

    @Override
    public String read(Object value) {
        try {
            return value instanceof PgSQLXML ? ((PgSQLXML) value).getString() : value instanceof String ? (String) value : null;
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        }
    }
}
