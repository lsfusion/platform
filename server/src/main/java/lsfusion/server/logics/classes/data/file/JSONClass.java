package lsfusion.server.logics.classes.data.file;

import lsfusion.interop.classes.DataType;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class JSONClass extends AJSONClass {

    public JSONClass() {
        super(LocalizedString.create("{classes.json}"));
    }

    public final static JSONClass instance = new JSONClass();

    @Override
    public String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, Type typeFrom, CastType castType) {
        if (typeFrom instanceof StaticFormatFileClass) {
            return "cast_static_file_to_json(" + value + ")";
        } else if (typeFrom instanceof DynamicFormatFileClass) {
            return "cast_dynamic_file_to_json(" + value + ")";
        }
        return super.getCast(value, syntax, typeEnv, typeFrom, castType);
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