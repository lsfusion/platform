package platform.server.data.type;

import platform.interop.Data;
import platform.server.classes.*;
import platform.server.logics.BusinessLogics;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class TypeSerializer {

    public static void serializeType(DataOutputStream outStream, Type type) throws IOException {
        if(type instanceof DataClass) {
            outStream.writeBoolean(false);
            ((DataClass)type).serialize(outStream);
        } else
            outStream.writeBoolean(true);
    }

    public static void serializeValueClass(DataOutputStream outStream, ValueClass cls) throws IOException {
        if (cls == null)
            outStream.writeBoolean(true);
        else {
            outStream.writeBoolean(false);
            cls.serialize(outStream);
        }
    }

    public static Type deserializeType(DataInputStream inStream) throws IOException {
        if(inStream.readBoolean())
            return ObjectType.instance;
        else
            return DataClass.deserialize(inStream);
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
        if (type == Data.IMAGE) return ImageClass.instance;
        if (type == Data.WORD) return WordClass.instance;
        if (type == Data.EXCEL) return ExcelClass.instance;
        if (type == Data.TEXT) return TextClass.instance;
        if (type == Data.YEAR) return YearClass.instance;
        if (type == Data.OBJECT) return context.baseClass.findClassID(inStream.readInt());
        if (type == Data.ACTION) return ActionClass.instance;
        if (type == Data.PDF) return PDFClass.instance;
        if (type == Data.DATETIME) return DateTimeClass.instance;
        if (type == Data.CUSTOMFILECLASS) return CustomFileClass.instance;

        //todo:!!
        if (type == Data.FILEACTION) return FileActionClass.getInstance("", "");
        //todo:!!
        if (type == Data.CLASSACTION) return new ClassActionClass(null, null);
        if (type == Data.CUSTOMFILEACTION) return CustomFileActionClass.instance;

        throw new IOException();
    }
}
