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

    public static Type deserializeType(DataInputStream inStream, int version) throws IOException {
        if (version < 6) {
            if(inStream.readBoolean())
                return ObjectType.instance;
            else
                return deserializeDataClass(inStream, version);
        } else {
            switch (inStream.readByte()) {
                case 0:
                    return ObjectType.instance;
                case 1:
                    return deserializeDataClass(inStream, version);
                case 2:
            }       return deserializeConcatenateType(inStream, version);
        }
    }

    public static ConcatenateType deserializeConcatenateType(DataInputStream inStream, int version) throws IOException {
        int typesCount = inStream.readInt();

        Type[] types = new Type[typesCount];

        for (int i = 0; i < typesCount; i++)
            types[i] = TypeSerializer.deserializeType(inStream, version);

        return ConcatenateType.get(types);
    }

    public static DataClass deserializeDataClass(DataInputStream inStream, int version) throws IOException {
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

        if (type == Data.VARSTRING) return StringClass.getv(inStream.readBoolean(), inStream.readInt());

        if (version < 7) {
            if (type == Data.STRING) return StringClass.get(inStream.readInt());
            if (type == Data.INSENSITIVESTRING) return StringClass.geti(inStream.readInt());
            if (type == Data.TEXT) return TextClass.instance;
        } else {
            if (type == Data.STRING) return StringClass.get(inStream.readBoolean(), inStream.readInt());
            if (type == Data.INSENSITIVESTRING) {
                // в 7й версии тип INSENSITIVESTRING был удалён
                throw new IllegalStateException("Incorrect type id");
            }
            if (type == Data.TEXT) {
                boolean caseInsensitive = inStream.readBoolean();
                assert !caseInsensitive;
                return TextClass.instance;
            }
        }

        if(version>=2) { // обратная совместимость
            if (type == Data.IMAGE) return ImageClass.get(inStream.readBoolean(), version >= 4 ? inStream.readBoolean() : false);
            if (type == Data.WORD) return WordClass.get(inStream.readBoolean(), version >= 4 ? inStream.readBoolean() : false);
            if (type == Data.EXCEL) return ExcelClass.get(inStream.readBoolean(), version >= 4 ? inStream.readBoolean() : false);
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
                return CustomStaticFormatFileClass.get(inStream.readBoolean(), version >= 4 ? inStream.readBoolean() : false, filterDescription, filterExtensions);
            }
            if (type == Data.DYNAMICFORMATFILE) return DynamicFormatFileClass.get(inStream.readBoolean(), version >= 4 ? inStream.readBoolean() : false);
            if (type == Data.PDF) return PDFClass.get(inStream.readBoolean(), version >= 4 ? inStream.readBoolean() : false);
        } else {
            if (type == Data.IMAGE) return ImageClass.get(false, false);
            if (type == Data.WORD) return WordClass.get(false, false);
            if (type == Data.EXCEL) return ExcelClass.get(false, false);
            if (type == Data.DYNAMICFORMATFILE) return DynamicFormatFileClass.get(false, false);
            if (type == Data.PDF) return PDFClass.get(false, false);
        }

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
        if (type == Data.STRING) return StringClass.get(inStream.readBoolean(), inStream.readInt());
        if (type == Data.VARSTRING) return StringClass.getv(inStream.readBoolean(), inStream.readInt());
        if (type == Data.INSENSITIVESTRING) {
            // в 7й версии тип INSENSITIVESTRING был удалён
            throw new IllegalStateException("Incorrect type id");
        }
        if (type == Data.TEXT) {
            boolean caseInsensitive = inStream.readBoolean();
            assert !caseInsensitive;
            return TextClass.instance;
        }
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
