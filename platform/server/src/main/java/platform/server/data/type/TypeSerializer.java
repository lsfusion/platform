package platform.server.data.type;

import platform.interop.Data;
import platform.server.classes.*;
import platform.server.logics.BusinessLogics;

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

    public static void serializeValueClass(DataOutputStream outStream, ValueClass cls) throws IOException {
        if (cls == null)
            outStream.writeBoolean(true);
        else {
            outStream.writeBoolean(false);
            cls.serialize(outStream);
        }
    }

    public static Type deserializeType(DataInputStream inStream, int version) throws IOException {
        if (version < 6) {
            if(inStream.readBoolean())
                return ObjectType.instance;
            else
                return DataClass.deserialize(inStream, version);
        } else {
            switch (inStream.readByte()) {
                case 0:
                    return ObjectType.instance;
                case 1:
                    return DataClass.deserialize(inStream, version);
                case 2:
            }       return ConcatenateType.deserialize(inStream, version);
        }
    }

    public static ValueClass deserializeValueClass(BusinessLogics context, DataInputStream inStream) throws IOException {
        byte type = inStream.readByte();

        if (type == Data.INTEGER) return IntegerClass.instance;
        if (type == Data.LONG) return LongClass.instance;
        if (type == Data.DOUBLE) return DoubleClass.instance;
        if (type == Data.NUMERIC) return NumericClass.get(inStream.readInt(), inStream.readInt());
        if (type == Data.LOGICAL) return LogicalClass.instance;
        if (type == Data.DATE) return DateClass.instance;
        if (type == Data.STRING) return StringClass.get(inStream.readInt());
        if (type == Data.INSENSITIVESTRING) return InsensitiveStringClass.get(inStream.readInt());
        if (type == Data.TEXT) return TextClass.instance;
        if (type == Data.YEAR) return YearClass.instance;
        if (type == Data.OBJECT) return context.LM.baseClass.findClassID(inStream.readInt());
        if (type == Data.ACTION) return ActionClass.instance;
        if (type == Data.DATETIME) return DateTimeClass.instance;
        if (type == Data.DYNAMICFORMATFILE) return DynamicFormatFileClass.instance;
        if (type == Data.TIME) return TimeClass.instance;
        if (type == Data.COLOR) return ColorClass.instance;

        if (type == Data.IMAGE) return ImageClass.instance;
        if (type == Data.WORD) return WordClass.instance;
        if (type == Data.EXCEL) return ExcelClass.instance;
        if (type == Data.PDF) return PDFClass.instance;
        //todo:!!
        if (type == Data.CUSTOMSTATICFORMATFILE) return CustomStaticFormatFileClass.getDefinedInstance(false, "", "");

        throw new IOException();
    }
}
