package lsfusion.gwt.shared.form.view.classes;

import lsfusion.gwt.shared.form.view.classes.link.*;

public interface GTypeVisitor<R> {

    R visit(GJSONType type);
    R visit(GTableType type);
    R visit(GPDFType type);
    R visit(GHTMLType type);
    R visit(GImageType type);
    R visit(GWordType type);
    R visit(GExcelType type);
    R visit(GCSVType type);
    R visit(GXMLType type);
    R visit(GCustomDynamicFormatFileType type);
    R visit(GCustomStaticFormatFileType type);

    R visit(GLinkType type);
    R visit(GJSONLinkType type);
    R visit(GTableLinkType type);
    R visit(GPDFLinkType type);
    R visit(GHTMLLinkType type);
    R visit(GImageLinkType type);
    R visit(GWordLinkType type);
    R visit(GExcelLinkType type);
    R visit(GCSVLinkType type);
    R visit(GXMLLinkType type);
    R visit(GCustomDynamicFormatLinkType type);
    R visit(GCustomStaticFormatLinkType type);

    R visit(GActionType type);
    R visit(GColorType type);
    R visit(GStringType type);
    R visit(GLogicalType type);
    R visit(GDateType type);
    R visit(GTimeType type);
    R visit(GDateTimeType type);

    R visit(GNumericType type);
    R visit(GLongType type);
    R visit(GDoubleType type);
    R visit(GIntegerType type);
    R visit(GObjectType type);

}