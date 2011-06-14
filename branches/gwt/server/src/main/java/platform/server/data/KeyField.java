package platform.server.data;

import platform.server.classes.IntegerClass;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.Type;

import java.io.DataInputStream;
import java.io.IOException;

public class KeyField extends Field implements Comparable<KeyField> {
    public KeyField(String name, Type type) {super(name,type);}

    public static KeyField dumb = new KeyField("dumb", IntegerClass.instance) {
        @Override
        public String getDeclare(SQLSyntax syntax) {
            return super.getDeclare(syntax) + " default 0";
        }
    };
    public KeyField(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    byte getType() {
        return 0;
    }

    public int compareTo(KeyField o) {
        return name.compareTo(o.name); 
    }
}
