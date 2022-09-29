package lsfusion.server.data.type.parse;

import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.logics.classes.data.LogicalClass;

public abstract class LogicalParseInterface extends StringParseInterface implements ValueParseInterface {

    public abstract boolean isTrue();

    @Override
    public Object getValue() {
        return isTrue() ? true : null;
    }

    public String getString(SQLSyntax syntax, StringBuilder envString, boolean usedRecursion) {
        return isTrue()
                ? LogicalClass.instance.getString(true, syntax)
                : SQLSyntax.NULL;
    }

}
