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
    public String getCastFromStatic(String value) {
        return "cast_static_file_to_json_text(" + value + ")";
    }

    @Override
    public String getCastToStatic(String value) {
        return "cast_json_text_to_static_file(" + value + ")";
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