package lsfusion.server.logics.classes.data.file;

import lsfusion.interop.classes.DataType;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class JSONClass extends AJSONClass {

    public JSONClass() {
        super(LocalizedString.create("{classes.json}"));
    }

    public final static JSONClass instance = new JSONClass();

    @Override
    public String getCastFromStatic(String value) {
        return "cast_static_file_to_json(" + value + ")";
    }

    @Override
    public String getCastToStatic(String value) {
        return "cast_json_to_static_file(" + value + ")";
    }

    static {
        DataClass.storeClass(instance);
    }

    @Override
    public String getDBString(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return syntax.getJSON();
    }

    @Override
    public String getSID() {
        return "JSON";
    }

    @Override
    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof JSONClass ? this : null;
    }

    @Override
    public byte getTypeID() {
        return DataType.JSON;
    }
}