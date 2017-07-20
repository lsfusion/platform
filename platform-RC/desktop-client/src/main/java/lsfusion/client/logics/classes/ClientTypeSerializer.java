package lsfusion.client.logics.classes;

import lsfusion.base.ExtInt;
import lsfusion.client.logics.classes.link.*;
import lsfusion.interop.Data;

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

        if (type == Data.OBJECT) return ClientObjectClass.deserialize(inStream);
        if (type == Data.INTEGER) return ClientIntegerClass.instance;
        if (type == Data.LONG) return ClientLongClass.instance;
        if (type == Data.DOUBLE) return ClientDoubleClass.instance;
        if (type == Data.NUMERIC) return new ClientNumericClass(inStream.readInt(), inStream.readInt());
        if (type == Data.LOGICAL) return ClientLogicalClass.instance;
        if (type == Data.DATE) return ClientDateClass.instance;
        if (type == Data.STRING) return new ClientStringClass(inStream.readBoolean(), inStream.readBoolean(), inStream.readBoolean(), ExtInt.deserialize(inStream));
        if (type == Data.YEAR) return ClientIntegerClass.instance;
        if (type == Data.DATETIME) return ClientDateTimeClass.instance;
        if (type == Data.TIME) return ClientTimeClass.instance;
        if (type == Data.COLOR) return ClientColorClass.instance;

        if (type == Data.PDF) return new ClientPDFClass(inStream.readBoolean(), inStream.readBoolean());
        if (type == Data.IMAGE) return new ClientImageClass(inStream.readBoolean(), inStream.readBoolean());
        if (type == Data.WORD) return new ClientWordClass(inStream.readBoolean(), inStream.readBoolean());
        if (type == Data.EXCEL) return new ClientExcelClass(inStream.readBoolean(), inStream.readBoolean());
        if (type == Data.DYNAMICFORMATFILE) return new ClientDynamicFormatFileClass(inStream.readBoolean(), inStream.readBoolean());
        if (type == Data.CUSTOMSTATICFORMATFILE) return ClientCustomStaticFormatFileClass.deserialize(inStream);

        if (type == Data.PDFLINK) return new ClientPDFLinkClass(inStream.readBoolean());
        if (type == Data.IMAGELINK) return new ClientImageLinkClass(inStream.readBoolean());
        if (type == Data.WORDLINK) return new ClientWordLinkClass(inStream.readBoolean());
        if (type == Data.EXCELLINK) return new ClientExcelLinkClass(inStream.readBoolean());
        if (type == Data.DYNAMICFORMATLINK) return new ClientDynamicFormatLinkClass(inStream.readBoolean());
        if (type == Data.CUSTOMSTATICFORMATLINK) return ClientCustomStaticFormatLinkClass.deserialize(inStream);

        if (type == Data.ACTION) return ClientActionClass.instance;

        throw new IOException();
    }
}
