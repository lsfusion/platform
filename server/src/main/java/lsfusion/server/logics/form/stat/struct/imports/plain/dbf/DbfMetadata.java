// copied from net.iryndin jdbf (https://github.com/iryndin/jdbf, Apache License 2.0) and stripped down to what the DBF import uses
package lsfusion.server.logics.form.stat.struct.imports.plain.dbf;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DbfMetadata {

    public enum Type {
        FoxBASE1(0x02,"FoxBASE"),
        FoxBASEPlus1(0x03,"FoxBASE+/Dbase III plus, no memo"),
        VisualFoxPro1(0x30, "Visual FoxPro"),
        VisualFoxPro2(0x31,"Visual FoxPro, autoincrement enabled"),
        dBASEIV1(0x43,"dBASE IV SQL table files, no memo"),
        dBASEIV2(0x63,"dBASE IV SQL system files, no memo"),
        FoxBASEPlus2(0x83,"FoxBASE+/dBASE III PLUS, with memo"),
        dBASEIV3(0x8B,"dBASE IV with memo"),
        dBASEIV4(0xCB,"dBASE IV SQL table files, with memo"),
        FoxPro2x(0xF5,"FoxPro 2.x (or earlier) with memo"),
        FoxBASE2(0xFB,"FoxBASE"),
        dBASEVII1(0x44,"dBASE VII SQL table files, no memo"),
        dBASEVII2(0x64,"dBASE VII SQL system files, no memo"),
        dBASEIVII3(0x8D,"dBASE VII with memo"),
        dBASEIVII4(0xCD,"dBASE VII SQL table files, with memo"),;

        final int type;
        final String description;

        Type(int type, String description) {
            this.type = type;
            this.description = description;
        }

        public static Type fromInt(byte bType) {
            int iType = 0xFF & bType;
            for (Type e : Type.values()) {
                if (e.type == iType) {
                    return e;
                }
            }
            return null;
        }
    }

    private Type type;
    private int fullHeaderLength;
    private int oneRecordLength;
    private Map<String, DbfField> fieldMap;

    public void setType(Type type) throws IOException {
        if (type == null)
            throw new IOException("The file is corrupted or is not a dbf file");
        this.type = type;
    }
    public int getFullHeaderLength() {
        return fullHeaderLength;
    }
    public void setFullHeaderLength(int fullHeaderLength) {
        this.fullHeaderLength = fullHeaderLength;
    }
    public int getOneRecordLength() {
        return oneRecordLength;
    }
    public void setOneRecordLength(int oneRecordLength) {
        this.oneRecordLength = oneRecordLength;
    }
    public DbfField getField(String name) {
        return fieldMap.get(name);
    }
    public Collection<DbfField> getFields() {
        return fieldMap.values();
    }
    public void setFields(List<DbfField> fields) {
        fieldMap = new LinkedHashMap<>(fields.size() * 2);
        int offset = 1;
        for (DbfField f : fields) {
            // 1. count offset
            f.setOffset(offset);
            offset += f.getLength();
            // 2. put field into map
            fieldMap.put(f.getName(), f);
        }
    }
}
