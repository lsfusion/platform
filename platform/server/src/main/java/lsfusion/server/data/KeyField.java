package lsfusion.server.data;

import lsfusion.server.classes.IntegerClass;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.Type;

import java.io.DataInputStream;
import java.io.IOException;

public class KeyField extends Field implements Comparable<KeyField> {
    public KeyField(String name, Type type) {super(name,type);}

    public static KeyField dumb = new KeyField("dumb", IntegerClass.instance) {
        @Override
        public String getDeclare(SQLSyntax syntax, TypeEnvironment typeEnv) {
            return super.getDeclare(syntax, typeEnv) + " default 0";
        }
    };
    public KeyField(DataInputStream inStream, int version) throws IOException {
        super(inStream, version);
    }

    byte getType() {
        return 0;
    }

    public int compareTo(KeyField o) {
        return name.compareTo(o.name); 
    }
}
