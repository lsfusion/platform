package platform.server.data;

import platform.server.data.sql.SQLSyntax;
import platform.server.data.types.Type;

public class Field {
    public String Name;
    public Type type;

    Field(String iName,Type iType) {Name=iName;
        type =iType;}

    public String GetDeclare(SQLSyntax Syntax) {
        return Name + " " + type.getDB(Syntax);
    }

    public String toString() {
        return Name;
    }
}
