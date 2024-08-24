package lsfusion.server.logics.classes.data.file;

import lsfusion.interop.classes.DataType;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class JSONTextClass extends AJSONClass {

    public JSONTextClass() {
        super(LocalizedString.create("{classes.json.text}"));
    }

    public final static JSONTextClass instance = new JSONTextClass();

    @Override
    public String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, Type typeFrom, CastType castType) {
        if (typeFrom instanceof StaticFormatFileClass) {
            return "cast_static_file_to_json_text(" + value + ")";
        } else if (typeFrom instanceof DynamicFormatFileClass) {
            return "cast_dynamic_file_to_json_text(" + value + ")";
        }
        return super.getCast(value, syntax, typeEnv, typeFrom, castType);
    }

    static {
        DataClass.storeClass(instance);
    }

    @Override
    public String getDBString(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return syntax.getJSONText();
    }

    @Override
    public String getSID() {
        return "JSONTEXT";
    }

    @Override
    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof JSONTextClass ? this : null;
    }

    @Override
    public byte getTypeID() {
        return DataType.JSONTEXT;
    }
}