package lsfusion.server.data.type;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.interop.classes.DataType;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.logics.classes.data.*;
import lsfusion.server.logics.classes.data.file.*;
import lsfusion.server.logics.classes.data.integral.DoubleClass;
import lsfusion.server.logics.classes.data.integral.IntegerClass;
import lsfusion.server.logics.classes.data.integral.LongClass;
import lsfusion.server.logics.classes.data.integral.NumericClass;
import lsfusion.server.logics.classes.data.link.*;
import lsfusion.server.logics.classes.data.time.*;
import lsfusion.server.logics.form.stat.struct.plain.JDBCTable;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

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
     * номер последней версии определён в {@link DBManager.DBStructure#DBStructure(DBManager.MigrationVersion)}
     */
    public static DataClass deserializeDataClass(DataInputStream inStream) throws IOException {
        byte type = inStream.readByte();

        if (type == DataType.INTEGER) return IntegerClass.instance;
        if (type == DataType.LONG) return LongClass.instance;
        if (type == DataType.DOUBLE) return DoubleClass.instance;
        if (type == DataType.NUMERIC) return NumericClass.get(ExtInt.deserialize(inStream), ExtInt.deserialize(inStream));
        if (type == DataType.LOGICAL) return LogicalClass.instance;
        if (type == DataType.TLOGICAL) return LogicalClass.threeStateInstance;
        if (type == DataType.DATE) return DateClass.instance;
        if (type == DataType.YEAR) return YearClass.instance;
        if (type == DataType.DATETIME) return DateTimeClass.instance;
        if (type == DataType.ZDATETIME) return ZDateTimeClass.instance;
        if (type == DataType.TIME) return TimeClass.instance;
        if (type == DataType.DATEINTERVAL) return IntervalClass.getInstance("DATE");
        if (type == DataType.TIMEINTERVAL) return IntervalClass.getInstance("TIME");
        if (type == DataType.DATETIMEINTERVAL) return IntervalClass.getInstance("DATETIME");
        if (type == DataType.COLOR) return ColorClass.instance;

        if (type == DataType.STRING || type == DataType.TEXT) {
            boolean blankPadded = inStream.readBoolean();
            boolean caseInsensitive = inStream.readBoolean();
            inStream.readBoolean(); // backward compatibility see StringClass.serialize
            ExtInt length = ExtInt.deserialize(inStream);
            if( type == DataType.TEXT)
                return inStream.readBoolean() ? TextClass.richInstance : TextClass.instance;
            return StringClass.get(blankPadded, caseInsensitive, length);
        }

        if (type == DataType.IMAGE) return ImageClass.get(inStream.readBoolean(), inStream.readBoolean());
        if (type == DataType.WORD) return WordClass.get(inStream.readBoolean(), inStream.readBoolean());
        if (type == DataType.EXCEL) return ExcelClass.get(inStream.readBoolean(), inStream.readBoolean());
        if (type == DataType.TXT) return TXTClass.get(inStream.readBoolean(), inStream.readBoolean());
        if (type == DataType.CSV) return CSVClass.get(inStream.readBoolean(), inStream.readBoolean());
        if (type == DataType.HTML) return HTMLClass.get(inStream.readBoolean(), inStream.readBoolean());
        if (type == DataType.JSON) return JSONClass.get(inStream.readBoolean(), inStream.readBoolean());
        if (type == DataType.XML) return XMLClass.get(inStream.readBoolean(), inStream.readBoolean());
        if (type == DataType.TABLE) return TableClass.get(inStream.readBoolean(), inStream.readBoolean());
        if (type == DataType.CUSTOMSTATICFORMATFILE) {
            boolean multiple = inStream.readBoolean();
            boolean storeName = inStream.readBoolean();
            String filterDescription = inStream.readUTF();
            ImSet<String> filterExtensions;
            int extCount = inStream.readInt();
            if (extCount <= 0) {
                filterExtensions = SetFact.singleton("");
            } else {
                MExclSet<String> mFilterExpressions = SetFact.mExclSet(extCount);
                for (int i = 0; i < extCount; ++i) {
                    mFilterExpressions.exclAdd(inStream.readUTF());
                }
                filterExtensions = mFilterExpressions.immutable();
            }
            return CustomStaticFormatFileClass.get(multiple, storeName, filterDescription, filterExtensions);
        }
        if (type == DataType.DYNAMICFORMATFILE) return DynamicFormatFileClass.get(inStream.readBoolean(), inStream.readBoolean());
        if (type == DataType.PDF) return PDFClass.get(inStream.readBoolean(), inStream.readBoolean());

        if (type == DataType.IMAGELINK) return ImageLinkClass.get(inStream.readBoolean());
        if (type == DataType.WORDLINK) return WordLinkClass.get(inStream.readBoolean());
        if (type == DataType.EXCELLINK) return ExcelLinkClass.get(inStream.readBoolean());
        if (type == DataType.TXTLINK) return TXTLinkClass.get(inStream.readBoolean());
        if (type == DataType.CSVLINK) return CSVLinkClass.get(inStream.readBoolean());
        if (type == DataType.HTMLLINK) return HTMLLinkClass.get(inStream.readBoolean());
        if (type == DataType.JSONLINK) return JSONLinkClass.get(inStream.readBoolean());
        if (type == DataType.XMLLINK) return XMLLinkClass.get(inStream.readBoolean());
        if (type == DataType.TABLELINK) return TableLinkClass.get(inStream.readBoolean());
        if (type == DataType.CUSTOMSTATICFORMATLINK) {
            boolean multiple = inStream.readBoolean();
            String filterDescription = inStream.readUTF();
            ImSet<String> filterExtensions;
            int extCount = inStream.readInt();
            if (extCount <= 0) {
                filterExtensions = SetFact.singleton("");
            } else {
                MExclSet<String> mFilterExtensions = SetFact.mExclSet(extCount);
                for (int i = 0; i < extCount; ++i)
                    mFilterExtensions.exclAdd(inStream.readUTF());
                filterExtensions = mFilterExtensions.immutable();
            }
            return CustomStaticFormatLinkClass.get(multiple, filterDescription, filterExtensions);
        }
        if (type == DataType.DYNAMICFORMATLINK) return DynamicFormatLinkClass.get(inStream.readBoolean());
        if (type == DataType.PDFLINK) return PDFLinkClass.get(inStream.readBoolean());
        if (type == DataType.DBFLINK) return DBFLinkClass.get(inStream.readBoolean());
        if (type == DataType.JDBC) return new JDBCTable.JDBCDataClass(inStream.readInt(), inStream.readUTF());

        throw new IOException();
    }

}
