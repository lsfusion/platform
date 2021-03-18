package lsfusion.gwt.server.convert;

import lsfusion.base.MIMETypeUtils;
import lsfusion.client.classes.ClientActionClass;
import lsfusion.client.classes.ClientObjectClass;
import lsfusion.client.classes.ClientObjectType;
import lsfusion.client.classes.data.*;
import lsfusion.client.classes.data.link.*;
import lsfusion.gwt.client.classes.GActionType;
import lsfusion.gwt.client.classes.GObjectClass;
import lsfusion.gwt.client.classes.GObjectType;
import lsfusion.gwt.client.classes.data.*;
import lsfusion.gwt.client.classes.data.link.*;
import lsfusion.gwt.client.form.property.GExtInt;

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
        return new GNumericType(new GExtInt(clientNumericClass.precision.value), new GExtInt(clientNumericClass.scale.value));
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

    @Converter(from = ClientDateTimeIntervalClass.class)
    public GIntervalType convertIntervalClass(ClientDateTimeIntervalClass clientDateTimeIntervalClass) {
        return GIntervalType.getInstance(clientDateTimeIntervalClass.getIntervalType());
    }

    @Converter(from = ClientDateIntervalClass.class)
    public GIntervalType convertIntervalClass(ClientDateIntervalClass clientDateIntervalClass) {
        return GIntervalType.getInstance(clientDateIntervalClass.getIntervalType());
    }

    @Converter(from = ClientTimeIntervalClass.class)
    public GIntervalType convertIntervalClass(ClientTimeIntervalClass clientTimeIntervalClass) {
        return GIntervalType.getInstance(clientTimeIntervalClass.getIntervalType());
    }

    @Converter(from = ClientZDateTimeClass.class)
    public GZDateTimeType convertDateTimeClass(ClientZDateTimeClass clientDateTimeClass) {
        return GZDateTimeType.instance;
    }

    private <T extends GFileType> T initializeFileClass(ClientFileClass clientFileClass, T fileClass) {
        fileClass.multiple = clientFileClass.multiple;
        fileClass.storeName = clientFileClass.storeName;
        if (clientFileClass instanceof ClientStaticFormatFileClass) { 
            ArrayList<String> validContentTypes = new ArrayList<>();
            for (String extension : ((ClientStaticFormatFileClass) clientFileClass).getExtensions()) {
                if (extension != null && !extension.isEmpty() && !extension.equals("*.*") && !extension.equals("*")) {
                    validContentTypes.add(MIMETypeUtils.MIMETypeForFileExtension(extension.toLowerCase()));
                } else {
                    validContentTypes.add(extension);
                }
            }
            fileClass.validContentTypes = validContentTypes;
        }
        return fileClass;
    }

    @Converter(from = ClientPDFClass.class)
    public GPDFType convertPDFClass(ClientPDFClass pdfClass) {
        return initializeFileClass(pdfClass, new GPDFType());
    }

    @Converter(from = ClientImageClass.class)
    public GImageType convertImageClass(ClientImageClass imageClass) {
        return initializeFileClass(imageClass, new GImageType(imageClass.getExtension()));
    }

    @Converter(from = ClientWordClass.class)
    public GWordType convertWordClass(ClientWordClass wordClass) {
        return initializeFileClass(wordClass, new GWordType());
    }

    @Converter(from = ClientExcelClass.class)
    public GExcelType convertExcelClass(ClientExcelClass excelClass) {
        return initializeFileClass(excelClass, new GExcelType());
    }

    @Converter(from = ClientTXTClass.class)
    public GTXTType convertTXTClass(ClientTXTClass txtClass) {
        return initializeFileClass(txtClass, new GTXTType());
    }

    @Converter(from = ClientCSVClass.class)
    public GCSVType convertCSVClass(ClientCSVClass csvClass) {
        return initializeFileClass(csvClass, new GCSVType());
    }

    @Converter(from = ClientHTMLClass.class)
    public GHTMLType convertHTMLClass(ClientHTMLClass htmlClass) {
        return initializeFileClass(htmlClass, new GHTMLType());
    }
    
    @Converter(from = ClientJSONClass.class)
    public GJSONType convertJSONClass(ClientJSONClass jsonClass) {
        return initializeFileClass(jsonClass, new GJSONType());
    }

    @Converter(from = ClientXMLClass.class)
    public GXMLType convertXMLClass(ClientXMLClass xmlClass) {
        return initializeFileClass(xmlClass, new GXMLType());
    }

    @Converter(from = ClientTableClass.class)
    public GTableType convertTableClass(ClientTableClass tableClass) {
        return initializeFileClass(tableClass, new GTableType());
    }

    @Converter(from = ClientCustomStaticFormatFileClass.class)
    public GCustomStaticFormatFileType convertCustomStaticFormatFileClass(ClientCustomStaticFormatFileClass customClass) {
        GCustomStaticFormatFileType customFormatFileType = initializeFileClass(customClass, new GCustomStaticFormatFileType());
        customFormatFileType.description = customClass.filterDescription;
        return customFormatFileType;
    }

    @Converter(from = ClientDynamicFormatFileClass.class)
    public GCustomDynamicFormatFileType convertCustomDynamicFormatClass(ClientDynamicFormatFileClass customClass) {
        return initializeFileClass(customClass, new GCustomDynamicFormatFileType());
    }

    @Converter(from = ClientPDFLinkClass.class)
    public GPDFLinkType convertPDFClass(ClientPDFLinkClass pdfClass) {
        return initializeLinkClass(pdfClass, new GPDFLinkType());
    }

    @Converter(from = ClientImageLinkClass.class)
    public GImageLinkType convertImageClass(ClientImageLinkClass imageClass) {
        return initializeLinkClass(imageClass, new GImageLinkType());
    }

    @Converter(from = ClientWordLinkClass.class)
    public GWordLinkType convertWordLinkClass(ClientWordLinkClass wordClass) {
        return initializeLinkClass(wordClass, new GWordLinkType());
    }

    @Converter(from = ClientExcelLinkClass.class)
    public GExcelLinkType convertExcelLinkClass(ClientExcelLinkClass excelClass) {
        return initializeLinkClass(excelClass, new GExcelLinkType());
    }

    @Converter(from = ClientTXTLinkClass.class)
    public GTXTLinkType convertTXTLinkClass(ClientTXTLinkClass txtClass) {
        return initializeLinkClass(txtClass, new GTXTLinkType());
    }

    @Converter(from = ClientCSVLinkClass.class)
    public GCSVLinkType convertCSVLinkClass(ClientCSVLinkClass csvClass) {
        return initializeLinkClass(csvClass, new GCSVLinkType());
    }

    @Converter(from = ClientHTMLLinkClass.class)
    public GHTMLLinkType convertHTMLLinkClass(ClientHTMLLinkClass htmlClass) {
        return initializeLinkClass(htmlClass, new GHTMLLinkType());
    }

    @Converter(from = ClientJSONLinkClass.class)
    public GJSONLinkType convertJSONLinkClass(ClientJSONLinkClass jsonClass) {
        return initializeLinkClass(jsonClass, new GJSONLinkType());
    }

    @Converter(from = ClientXMLLinkClass.class)
    public GXMLLinkType convertXMLLinkClass(ClientXMLLinkClass xmlClass) {
        return initializeLinkClass(xmlClass, new GXMLLinkType());
    }

    @Converter(from = ClientTableLinkClass.class)
    public GTableLinkType convertTableLinkClass(ClientTableLinkClass tableClass) {
        return initializeLinkClass(tableClass, new GTableLinkType());
    }

    @Converter(from = ClientCustomStaticFormatLinkClass.class)
    public GCustomStaticFormatLinkType convertCustomStaticFormatLinkClass(ClientCustomStaticFormatLinkClass customClass) {
        GCustomStaticFormatLinkType customFormatLinkType = initializeLinkClass(customClass, new GCustomStaticFormatLinkType());
        customFormatLinkType.description = customClass.filterDescription;
        return customFormatLinkType;
    }

    @Converter(from = ClientDynamicFormatLinkClass.class)
    public GCustomDynamicFormatLinkType convertCustomDynamicFormatClass(ClientDynamicFormatLinkClass customClass) {
        return initializeLinkClass(customClass, new GCustomDynamicFormatLinkType());
    }

    private <T extends GLinkType> T initializeLinkClass(ClientLinkClass clientLinkClass, T linkClass) {
        linkClass.multiple = clientLinkClass.multiple;
        return linkClass;
    }

    @Converter(from = ClientStringClass.class)
    public GStringType convertStringClass(ClientStringClass clientStringClass) {
        return new GStringType(new GExtInt(clientStringClass.length.value), clientStringClass.caseInsensitive, clientStringClass.blankPadded);
    }

    @Converter(from = ClientTextClass.class)
    public GTextType convertTextClass(ClientTextClass clientTextClass) {
        return new GTextType(clientTextClass.rich);
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
        ArrayList<GObjectClass> children = new ArrayList<>();
        for (ClientObjectClass child : clientClass.getChildren()) {
            children.add(convertObjectClass(child));
        }

        return new GObjectClass(clientClass.getID(), clientClass.isConcreate(), clientClass.getCaption(), children);
    }
}
