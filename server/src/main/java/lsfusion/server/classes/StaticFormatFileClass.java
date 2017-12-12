package lsfusion.server.classes;

import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.Type;

public abstract class StaticFormatFileClass extends FileClass {

    public abstract String getOpenExtension(byte[] file);

    protected StaticFormatFileClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    public String getDefaultCastExtension() {
        return null;
    }
    
    @Override
    public String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, Type typeFrom) {
        if (typeFrom instanceof DynamicFormatFileClass) {
            return "castfromcustomfile(" + value + ")";
        }
        return super.getCast(value, syntax, typeEnv, typeFrom);
    }

    @Override
    public byte[] format(byte[] value) {
        return value;
    }
}
