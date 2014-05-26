package lsfusion.server.data.type;

import lsfusion.server.classes.LogicalClass;
import lsfusion.server.data.sql.SQLSyntax;

public abstract class LogicalParseInterface extends StringParseInterface {

    public abstract boolean isTrue();

    public String getString(SQLSyntax syntax, StringBuilder envString, boolean usedRecursion) {
        return isTrue()
                ? LogicalClass.instance.getString(true, syntax)
                : SQLSyntax.NULL;
    }

}
