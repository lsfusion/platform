package lsfusion.server.logics.classes.data.file;

import lsfusion.interop.classes.DataType;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class JSONStringClass extends AJSONClass {

    public JSONStringClass() {
        super(LocalizedString.create("{classes.json.string}"));
    }

    public final static JSONStringClass instance = new JSONStringClass();

    @Override
    public String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, Type typeFrom, boolean isArith) {
        if (typeFrom instanceof StaticFormatFileClass) {
            return "cast_static_file_to_json_string(" + value + ")";
        } else if (typeFrom instanceof DynamicFormatFileClass) {
            return "cast_dynamic_file_to_json_string(" + value + ")";
        }
        return super.getCast(value, syntax, typeEnv, typeFrom, isArith);
    }

    static {
        DataClass.storeClass(instance);
    }

    @Override
    public String getDBString(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return syntax.getJSONString();
    }

    @Override
    public String getSID() {
        return "JSONSTRING";
    }

    @Override
    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof JSONStringClass ? this : null;
    }

    @Override
    public byte getTypeID() {
        return DataType.JSONSTRING;
    }
}