package lsfusion.server.data.type;

import lsfusion.base.ExtInt;
import lsfusion.interop.Data;
import lsfusion.server.classes.*;
import lsfusion.server.logics.BusinessLogics;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class TypeSerializer {
    public static byte[] serializeType(Type type) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);
        serializeType(dataStream, type);
        return outStream.toByteArray();
    }

    public static void serializeType(DataOutputStream outStream, Type type) throws IOException {
        if (type instanceof ObjectType)
            outStream.writeByte(0);
        else if (type instanceof DataClass) {
            outStream.writeByte(1);
            ((DataClass) type).serialize(outStream);
        } else if (type instanceof ConcatenateType) {
            outStream.writeByte(2);
            ((ConcatenateType) type).serialize(outStream);
        }
    }

    public static Type deserializeType(DataInputStream inStream) throws IOException {
        switch (inStream.readByte()) {
            case 0:
                return ObjectType.instance;
            case 1:
                return deserializeDataClass(inStream);
            case 2:
        }       return deserializeConcatenateType(inStream);
    }

    public static ConcatenateType deserializeConcatenateType(DataInputStream inStream) throws IOException {
        int typesCount = inStream.readInt();

        Type[] types = new Type[typesCount];

        for (int i = 0; i < typesCount; i++)
            types[i] = TypeSerializer.deserializeType(inStream);

        return ConcatenateType.get(types);
    }

    /**
     * номер последней версии определён в {@link lsfusion.server.logics.DBManager.DBStructure#DBStructure(lsfusion.server.logics.DBManager.DBVersion)}
     */
    public static DataClass deserializeDataClass(DataInputStream inStream) throws IOException {
        byte type = inStream.readByte();

        if (type == Data.INTEGER) return IntegerClass.instance;
        if (type == Data.LONG) return LongClass.instance;
        if (type == Data.DOUBLE) return DoubleClass.instance;
        if (type == Data.NUMERIC) return NumericClass.get(inStream.readInt(), inStream.readInt());
        if (type == Data.LOGICAL) return LogicalClass.instance;
        if (type == Data.DATE) return DateClass.instance;
        if (type == Data.YEAR) return YearClass.instance;
        if (type == Data.DATETIME) return DateTimeClass.instance;
        if (type == Data.TIME) return TimeClass.instance;
        if (type == Data.COLOR) return ColorClass.instance;

        
        if (type == Data.STRING) {
            return StringClass.get(inStream.readBoolean(), inStream.readBoolean(), inStream.readBoolean(), ExtInt.deserialize(inStream));
        }

        if (type == Data.IMAGE) return ImageClass.get(inStream.readBoolean(), inStream.readBoolean());
        if (type == Data.WORD) return WordClass.get(inStream.readBoolean(), inStream.readBoolean());
        if (type == Data.EXCEL) return ExcelClass.get(inStream.readBoolean(), inStream.readBoolean());
        if (type == Data.CUSTOMSTATICFORMATFILE) {
            String filterDescription = inStream.readUTF();
            String[] filterExtensions;
            int extCount = inStream.readInt();
            if (extCount <= 0) {
                filterExtensions = new String[1];
                filterExtensions[0] = "*";
            } else {
                filterExtensions = new String[extCount];

                for (int i = 0; i < extCount; ++i) {
                    filterExtensions[i] = inStream.readUTF();
                }
            }
            return CustomStaticFormatFileClass.get(inStream.readBoolean(), inStream.readBoolean(), filterDescription, filterExtensions);
        }
        if (type == Data.DYNAMICFORMATFILE) return DynamicFormatFileClass.get(inStream.readBoolean(), inStream.readBoolean());
        if (type == Data.PDF) return PDFClass.get(inStream.readBoolean(), inStream.readBoolean());

        throw new IOException();
    }

    public static ValueClass deserializeValueClass(BusinessLogics context, DataInputStream inStream) throws IOException {
        byte type = inStream.readByte();

        if (type == Data.INTEGER) return IntegerClass.instance;
        if (type == Data.LONG) return LongClass.instance;
        if (type == Data.DOUBLE) return DoubleClass.instance;
        if (type == Data.NUMERIC) return NumericClass.get(inStream.readInt(), inStream.readInt());
        if (type == Data.LOGICAL) return LogicalClass.instance;
        if (type == Data.DATE) return DateClass.instance;
        if (type == Data.STRING) return StringClass.get(inStream.readBoolean(), inStream.readBoolean(), inStream.readBoolean(), ExtInt.deserialize(inStream));
        if (type == Data.YEAR) return YearClass.instance;
        if (type == Data.OBJECT) return context.LM.baseClass.findClassID(inStream.readInt());
        if (type == Data.ACTION) return ActionClass.instance;
        if (type == Data.DATETIME) return DateTimeClass.instance;
        if (type == Data.DYNAMICFORMATFILE) return DynamicFormatFileClass.get(false, false);
        if (type == Data.TIME) return TimeClass.instance;
        if (type == Data.COLOR) return ColorClass.instance;

        if (type == Data.IMAGE) return ImageClass.get(false, false);
        if (type == Data.WORD) return WordClass.get(false, false);
        if (type == Data.EXCEL) return ExcelClass.get(false, false);
        if (type == Data.PDF) return PDFClass.get(false, false);
        //todo:!!
        if (type == Data.CUSTOMSTATICFORMATFILE) return CustomStaticFormatFileClass.get(false, false, "", "");

        throw new IOException();
    }
}
