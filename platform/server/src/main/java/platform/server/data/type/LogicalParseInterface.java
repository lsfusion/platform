package platform.server.data.type;

import platform.server.classes.LogicalClass;
import platform.server.data.sql.SQLSyntax;

public abstract class LogicalParseInterface extends StringParseInterface {

    public abstract boolean isTrue();

    public String getString(SQLSyntax syntax) {
        return isTrue()
                ? LogicalClass.instance.getString(true, syntax)
                : SQLSyntax.NULL;
    }

}
