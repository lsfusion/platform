package platform.server.classes;

import platform.interop.Data;
import platform.server.logics.BusinessLogics;

import java.io.DataInputStream;
import java.io.IOException;

public class ClassSerializer {
    public static ValueClass deserialize(BusinessLogics context, DataInputStream inStream) throws IOException {
        byte type = inStream.readByte();

        if (type == Data.INTEGER) return IntegerClass.instance;
        if (type == Data.LONG) return LongClass.instance;
        if (type == Data.DOUBLE) return DoubleClass.instance;
        if (type == Data.NUMERIC) return NumericClass.get(inStream.readInt(), inStream.readInt());
        if (type == Data.LOGICAL) return LogicalClass.instance;
        if (type == Data.DATE) return DateClass.instance;
        if (type == Data.STRING) return StringClass.get(inStream.readInt());
        if (type == Data.IMAGE) return ImageClass.instance;
        if (type == Data.WORD) return WordClass.instance;
        if (type == Data.EXCEL) return ExcelClass.instance;
        if (type == Data.TEXT) return TextClass.instance;
        if (type == Data.YEAR) return YearClass.instance;
        if (type == Data.OBJECT) return context.baseClass.findClassID(inStream.readInt());
        if (type == Data.ACTION) return ActionClass.instance;

        //todo:!!
        if (type == Data.FILEACTION) return FileActionClass.getInstance("", "");
        //todo:!!
        if (type == Data.CLASSACTION) return new ClassActionClass(null, null);

        throw new IOException();
    }
}
