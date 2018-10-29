package lsfusion.server.logics.property.actions.integration.exporting.plain.table;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.TypeSerializer;
import lsfusion.server.logics.property.actions.integration.exporting.plain.ExportByteArrayPlainWriter;
import lsfusion.server.logics.property.actions.integration.exporting.plain.ExportPlainWriter;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ExportTableWriter extends ExportByteArrayPlainWriter {
    
    private final boolean singleRow;

    private final DataOutputStream o;
            
    public ExportTableWriter(ImOrderMap<String, Type> fieldTypes, boolean singleRow) throws IOException {
        super(fieldTypes);
        this.singleRow = singleRow;

        o = new DataOutputStream(outputStream);
        o.writeBoolean(singleRow); //singleRow

        o.writeInt(fieldTypes.size());
        for(int i = 0,size=fieldTypes.size();i<size;i++) {
            BaseUtils.serializeString(o, fieldTypes.getKey(i));
            TypeSerializer.serializeType(o, fieldTypes.getValue(i));
        }
        o.writeBoolean(true); // fixed size
    }

    @Override
    public void writeCount(int count) throws IOException {
        o.writeInt(count);
    }

    private int count = 0;
    @Override
    public void writeLine(int rowNum, ImMap<String, Object> row) throws IOException {
        if(singleRow && count++ > 0)
            return;

        for (String field : fieldTypes.keyIt())
            BaseUtils.serializeObject(o, row.get(field));
    }

    @Override
    protected void closeWriter() throws IOException {
        o.close();
    }
}
