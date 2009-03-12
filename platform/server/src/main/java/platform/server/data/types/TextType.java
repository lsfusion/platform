package platform.server.data.types;

import platform.server.data.sql.SQLSyntax;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TextType extends StringType {

    public TextType() {
        super("T");
    }

    public String getDB(SQLSyntax syntax) {
        return syntax.getTextType();
    }

    byte getType() {
        return 6;
    }
}
