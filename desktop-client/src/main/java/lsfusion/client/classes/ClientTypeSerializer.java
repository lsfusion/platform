package lsfusion.client.classes;

import lsfusion.client.classes.data.*;
import lsfusion.client.classes.data.link.*;
import lsfusion.interop.classes.DataType;
import lsfusion.interop.form.property.ExtInt;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientTypeSerializer {

    public static ClientType deserializeClientType(byte[] typeData) throws IOException {
        return deserializeClientType(new DataInputStream(new ByteArrayInputStream(typeData)));
    }

    public static ClientType deserializeClientType(DataInputStream inStream) throws IOException {
        switch (inStream.readByte()) {
            case 0:
                return ClientObjectClass.type;
            case 1:
                return (ClientType) (deserializeClientClass(inStream));
            case 2:
                throw new UnsupportedOperationException("Concatenate Type is not supported yet");
        }
        throw new RuntimeException("Deserialize error");
    }

    public static void serializeClientType(DataOutputStream outStream, ClientType type) throws IOException {
        if (type instanceof ClientObjectClass)
            outStream.writeByte(0);
        else if (type instanceof ClientDataClass) {
            outStream.writeByte(1);
            ((ClientClass) type).serialize(outStream);
        } else
            throw new UnsupportedOperationException("Concatenate Type is not supported yet");
    }

    public static ClientObjectClass deserializeClientObjectClass(DataInputStream inStream) throws IOException {
        return (ClientObjectClass) deserializeClientClass(inStream);
    }

    public static ClientClass deserializeClientClass(DataInputStream inStream) throws IOException {
        return deserializeClientClass(inStream, false);
    }

    /**
     * по сути этот метод дублирует логику {@link lsfusion.server.data.type.TypeSerializer#deserializeDataClass(java.io.DataInputStream, int)} для последней версии ДБ
     */
    public static ClientClass deserializeClientClass(DataInputStream inStream, boolean nulls) throws IOException {

        if (nulls && inStream.readByte() == 0) return null;

        byte type = inStream.readByte();

        if (type == DataType.OBJECT) return ClientObjectClass.deserialize(inStream);
        if (type == DataType.INTEGER) return ClientIntegerClass.instance;
        if (type == DataType.LONG) return ClientLongClass.instance;
        if (type == DataType.DOUBLE) return ClientDoubleClass.instance;
        if (type == DataType.NUMERIC) return new ClientNumericClass(ExtInt.deserialize(inStream), ExtInt.deserialize(inStream));
        if (type == DataType.LOGICAL) return ClientLogicalClass.instance;
        if (type == DataType.TLOGICAL) return ClientLogicalClass.threeStateInstance;
        if (type == DataType.DATE) return ClientDateClass.instance;

        if (type == DataType.STRING || type == DataType.TEXT) {
            boolean blankPadded = inStream.readBoolean();
            boolean caseInsensitive = inStream.readBoolean();
            inStream.readBoolean(); // backward compatibility see StringClass.serialize
            ExtInt length = ExtInt.deserialize(inStream);
            if( type == DataType.TEXT)
                return new ClientTextClass(inStream.readBoolean());
            return new ClientStringClass(blankPadded, caseInsensitive, length);
        }

        if (type == DataType.YEAR) return ClientIntegerClass.instance;
        if (type == DataType.DATETIME) return ClientDateTimeClass.instance;
        if (type == DataType.ZDATETIME) return ClientZDateTimeClass.instance;
        if (type == DataType.DATEINTERVAL) return ClientIntervalClass.getInstance("DATE");
        if (type == DataType.DATETIMEINTERVAL) return ClientIntervalClass.getInstance("DATETIME");
        if (type == DataType.TIMEINTERVAL) return ClientIntervalClass.getInstance("TIME");
        if (type == DataType.ZDATETIMEINTERVAL) return ClientIntervalClass.getInstance("ZDATETIME");
        if (type == DataType.TIME) return ClientTimeClass.instance;
        if (type == DataType.COLOR) return ClientColorClass.instance;

        if (type == DataType.PDF) return new ClientPDFClass(inStream.readBoolean(), inStream.readBoolean());
        if (type == DataType.DBF) return new ClientDBFClass(inStream.readBoolean(), inStream.readBoolean());
        if (type == DataType.IMAGE) return new ClientImageClass(inStream.readBoolean(), inStream.readBoolean());
        if (type == DataType.WORD) return new ClientWordClass(inStream.readBoolean(), inStream.readBoolean());
        if (type == DataType.EXCEL) return new ClientExcelClass(inStream.readBoolean(), inStream.readBoolean());
        if (type == DataType.TXT) return new ClientTXTClass(inStream.readBoolean(), inStream.readBoolean());
        if (type == DataType.CSV) return new ClientCSVClass(inStream.readBoolean(), inStream.readBoolean());
        if (type == DataType.HTML) return new ClientHTMLClass(inStream.readBoolean(), inStream.readBoolean());
        if (type == DataType.JSON) return new ClientJSONClass(inStream.readBoolean(), inStream.readBoolean());
        if (type == DataType.XML) return new ClientXMLClass(inStream.readBoolean(), inStream.readBoolean());
        if (type == DataType.TABLE) return new ClientTableClass(inStream.readBoolean(), inStream.readBoolean());
        if (type == DataType.DYNAMICFORMATFILE) return new ClientDynamicFormatFileClass(inStream.readBoolean(), inStream.readBoolean());
        if (type == DataType.CUSTOMSTATICFORMATFILE) return ClientCustomStaticFormatFileClass.deserialize(inStream);

        if (type == DataType.PDFLINK) return new ClientPDFLinkClass(inStream.readBoolean());
        if (type == DataType.DBFLINK) return new ClientDBFLinkClass(inStream.readBoolean());
        if (type == DataType.IMAGELINK) return new ClientImageLinkClass(inStream.readBoolean());
        if (type == DataType.WORDLINK) return new ClientWordLinkClass(inStream.readBoolean());
        if (type == DataType.EXCELLINK) return new ClientExcelLinkClass(inStream.readBoolean());
        if (type == DataType.TXTLINK) return new ClientTXTLinkClass(inStream.readBoolean());
        if (type == DataType.CSVLINK) return new ClientCSVLinkClass(inStream.readBoolean());
        if (type == DataType.HTMLLINK) return new ClientHTMLLinkClass(inStream.readBoolean());
        if (type == DataType.JSONLINK) return new ClientJSONLinkClass(inStream.readBoolean());
        if (type == DataType.XMLLINK) return new ClientXMLLinkClass(inStream.readBoolean());
        if (type == DataType.TABLELINK) return new ClientTableLinkClass(inStream.readBoolean());
        if (type == DataType.DYNAMICFORMATLINK) return new ClientDynamicFormatLinkClass(inStream.readBoolean());
        if (type == DataType.CUSTOMSTATICFORMATLINK) return ClientCustomStaticFormatLinkClass.deserialize(inStream);

        if (type == DataType.ACTION) return ClientActionClass.instance;

        throw new IOException();
    }
}
