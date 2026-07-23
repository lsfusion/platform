// copied from net.iryndin jdbf (https://github.com/iryndin/jdbf, Apache License 2.0) and stripped down to what the DBF import uses
package lsfusion.server.logics.form.stat.struct.imports.plain.dbf;

public class BitUtils {
    public static int makeInt(byte b1, byte b2) {
        return ((b1 <<  0) & 0x000000FF) +
               ((b2 <<  8) & 0x0000FF00);
    }

    public static int makeInt(byte b1, byte b2, byte b3, byte b4) {
        return ((b1 <<  0) & 0x000000FF) +
               ((b2 <<  8) & 0x0000FF00) +
               ((b3 << 16) & 0x00FF0000) +
               ((b4 << 24) & 0xFF000000);
    }
}
