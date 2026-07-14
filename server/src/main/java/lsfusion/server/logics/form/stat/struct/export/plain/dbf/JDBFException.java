// replaces com.hexiong.jdbf.JDBFException from the unmaintained org.jdbf:jdbf artifact
package lsfusion.server.logics.form.stat.struct.export.plain.dbf;

public class JDBFException extends Exception {

    public JDBFException(String message) {
        super(message);
    }

    public JDBFException(Throwable cause) {
        super(cause);
    }

    public JDBFException(String message, Throwable cause) {
        super(message, cause);
    }
}
