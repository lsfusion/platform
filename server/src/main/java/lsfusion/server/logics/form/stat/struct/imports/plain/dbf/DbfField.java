// copied from net.iryndin jdbf (https://github.com/iryndin/jdbf, Apache License 2.0) and stripped down to what the DBF import uses
package lsfusion.server.logics.form.stat.struct.imports.plain.dbf;

public class DbfField {

    public enum Type {
        Character('C'),
        Currency('Y'),
        Numeric('N'),
        Float('F'),
        Date('D'),
        /**
         * @deprecated FoxPro-specific extension. Use Timestamp/@ with dBASE 7 or later
         */
        @Deprecated
        DateTime('T'),
        Timestamp('@'), // dbASE 7 julain date
        /**
         * @deprecated Binary doubles are FoxPro specific dBASE V uses B for binary MEMOs. Use Double7, Float or Numeric instead
         */
        @Deprecated
        Double('B'),
        Double7('O'), // dBASE 7 binary double (standardized in contrast to 'B'
        Integer('I'),
        Logical('L'),
        Memo('M'),
        General('G'),
        Picture('P'),
        NullFlags('0');

        final char type;

        Type(char type) {
            this.type = type;
        }

        public static Type fromChar(char type) {
            for (Type e : Type.values()) {
                if (e.type == type) {
                    return e;
                }
            }
            return null;
        }
    }

    private String name;
    private Type type;
    private int length;
    private int offset;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Type getType() {
        return type;
    }
    public void setType(Type type) {
        this.type = type;
    }
    public int getLength() {
        return length;
    }
    public void setLength(int length) {
        this.length = length;
    }
    public int getOffset() {
        return offset;
    }
    public void setOffset(int offset) {
        this.offset = offset;
    }
}
