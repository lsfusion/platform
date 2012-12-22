package platform.gwt.form.server.convert;

import platform.client.logics.classes.*;
import platform.gwt.form.shared.view.classes.*;

import java.util.ArrayList;

@SuppressWarnings("UnusedDeclaration")
public class ClientTypeToGwtConverter extends ObjectConverter {
    private static final class InstanceHolder {
        private static final ClientTypeToGwtConverter instance = new ClientTypeToGwtConverter();
    }

    public static ClientTypeToGwtConverter getInstance() {
        return InstanceHolder.instance;
    }

    private ClientTypeToGwtConverter() {
    }

    @Converter(from = ClientLongClass.class)
    public GLongType convertLongClass(ClientLongClass clientLongClass) {
        return GLongType.instance;
    }

    @Converter(from = ClientDoubleClass.class)
    public GDoubleType convertDoubleClass(ClientDoubleClass clientDoubleClass) {
        return GDoubleType.instance;
    }

    @Converter(from = ClientNumericClass.class)
    public GNumericType convertNumericClass(ClientNumericClass clientNumericClass) {
        return new GNumericType(clientNumericClass.length, clientNumericClass.precision);
    }

    @Converter(from = ClientLongClass.class)
    public GLongType convertLongClass(ClientActionClass clientActionClass) {
        return GLongType.instance;
    }

    @Converter(from = ClientIntegerClass.class)
    public GIntegerType convertIntegerClass(ClientIntegerClass clientIntegerClass) {
        return GIntegerType.instance;
    }

    @Converter(from = ClientActionClass.class)
    public GActionType convertActionClass(ClientActionClass clientActionClass) {
        return GActionType.instance;
    }

    @Converter(from = ClientTextClass.class)
    public GTextType convertTextClass(ClientTextClass clientTextClass) {
        return GTextType.instance;
    }

    @Converter(from = ClientLogicalClass.class)
    public GLogicalType convertLogicalClass(ClientLogicalClass clientLogicalClass) {
        return GLogicalType.instance;
    }

    @Converter(from = ClientTimeClass.class)
    public GTimeType convertTimeClass(ClientTimeClass clientTimeClass) {
        return GTimeType.instance;
    }

    @Converter(from = ClientDateTimeClass.class)
    public GDateTimeType convertDateTimeClass(ClientDateTimeClass clientDateTimeClass) {
        return GDateTimeType.instance;
    }

    @Converter(from = ClientPDFClass.class)
    public GPDFType convertPDFClass(ClientPDFClass pdfClass) {
        return GPDFType.instance;
    }

    @Converter(from = ClientImageClass.class)
    public GImageType convertImageClass(ClientImageClass imageClass) {
        return GImageType.instance;
    }

    @Converter(from = ClientWordClass.class)
    public GWordType convertWordClass(ClientWordClass wordClass) {
        return GWordType.instance;
    }

    @Converter(from = ClientExcelClass.class)
    public GExcelType convertExcelClass(ClientExcelClass excelClass) {
        return GExcelType.instance;
    }

    @Converter(from = ClientCustomStaticFormatFileClass.class)
    public GCustomStaticFormatFileType convertCustomStaticFormatFileClass(ClientCustomStaticFormatFileClass customClass) {
        return GCustomStaticFormatFileType.instance;
    }

    @Converter(from = ClientDynamicFormatFileClass.class)
    public GCustomDynamicFormatFileType convertCustomDynamicFormatClass(ClientDynamicFormatFileClass customClass) {
        return GCustomDynamicFormatFileType.instance;
    }

    @Converter(from = ClientStringClass.class)
    public GStringType convertStringClass(ClientStringClass clientStringClass) {
        return new GStringType(clientStringClass.length);
    }

    @Converter(from = ClientInsensitiveStringClass.class)
    public GInsensitiveStringType convertIntegerClass(ClientInsensitiveStringClass clientInsensitiveStringClass) {
        return new GInsensitiveStringType(clientInsensitiveStringClass.length);
    }

    @Converter(from = ClientDateClass.class)
    public GDateType convertDateClass(ClientDateClass clientDateClass) {
        return GDateType.instance;
    }

    @Converter(from = ClientColorClass.class)
    public GColorType convertColorClass(ClientColorClass clientColorClass) {
        return GColorType.instance;
    }

    @Converter(from = ClientObjectType.class)
    public GObjectType convertObjectType(ClientObjectType clientObjectType) {
        return GObjectType.instance;
    }

    @Converter(from = ClientObjectClass.class)
    public GObjectClass convertObjectClass(ClientObjectClass clientClass) {
        ArrayList<GObjectClass> children = new ArrayList<GObjectClass>();
        for (ClientObjectClass child : clientClass.getChildren()) {
            children.add(convertObjectClass(child));
        }

        return new GObjectClass(clientClass.getID(), clientClass.isConcreate(), clientClass.getCaption(), children);
    }
}
