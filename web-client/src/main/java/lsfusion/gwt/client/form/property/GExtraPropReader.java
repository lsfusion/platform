package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;

import static lsfusion.gwt.client.GFormChanges.GPropertyReadType.*;

public class GExtraPropReader extends GExtraPropertyReader {

    private int readerType;

    public GExtraPropReader() {
    }

    public GExtraPropReader(int readerID, int groupObjectID, int readerType) {
        super(readerID, groupObjectID, getPrefix(readerType));
        this.readerType = readerType;
    }

    public void update(GPropertyController controller, NativeHashMap<GGroupObjectValue, PValue> values) {
        if(readerType == CELL_FONT) {
            controller.updateCellFontValues(this, values);
        } else if (readerType == COMMENT) {
            controller.updatePropertyComments(this, values);
        } else if (readerType == COMMENTELEMENTCLASS) {
            controller.updateCellCommentElementClasses(this, values);
        } else if (readerType == PLACEHOLDER) {
            controller.updatePlaceholderValues(this, values);
        } else if (readerType == PATTERN) {
            controller.updatePatternValues(this, values);
        } else if (readerType == REGEXP) {
            controller.updateRegexpValues(this, values);
        } else if (readerType == REGEXPMESSAGE) {
            controller.updateRegexpMessageValues(this, values);
        } else if (readerType == TOOLTIP) {
            controller.updateTooltipValues(this, values);
        } else if (readerType == VALUETOOLTIP) {
            controller.updateValueTooltipValues(this, values);
        } else if (readerType == PROPERTY_CUSTOM_OPTIONS) {
            controller.updatePropertyCustomOptionsValues(this, values);
        } else if (readerType == CHANGEKEY) {
            controller.updateChangeKeyValues(this, values);
        } else if (readerType == CHANGEMOUSE) {
            controller.updateChangeMouseValues(this, values);
        } else if(readerType == CAPTIONELEMENTCLASS) {
            controller.updateCellCaptionElementClasses(this, values);
        } else if(readerType == FOOTERELEMENTCLASS) {
            controller.updateCellFooterElementClasses(this, values);
        } else if (readerType == DEFAULTVALUE) {
            controller.updateDefaultValueValues(this, values);
        }
    }

    @Override
    public String getMetaField() {
        if (readerType == COMMENT) return "comment";
        if (readerType == PLACEHOLDER) return "placeholder";
        if (readerType == PATTERN) return "pattern";
        if (readerType == REGEXP) return "regexp";
        if (readerType == REGEXPMESSAGE) return "regexpMessage";
        if (readerType == TOOLTIP) return "tooltip";
        if (readerType == VALUETOOLTIP) return "valueTooltip";
        if (readerType == PROPERTY_CUSTOM_OPTIONS) return "customOptions";
        if (readerType == DEFAULTVALUE) return "defaultValue";
        return null; // CHANGEKEY/CHANGEMOUSE/... are not projected into data.meta
    }

    @Override
    public GMetaConverter getMetaConverter() {
        if (readerType == COMMENT || readerType == TOOLTIP) return GMetaConverter.TEXT; // both are a trimmed string
        if (readerType == PROPERTY_CUSTOM_OPTIONS) return GMetaConverter.JSON;
        return GMetaConverter.STRING; // PLACEHOLDER/PATTERN/REGEXP/REGEXPMESSAGE/VALUETOOLTIP/DEFAULTVALUE
    }

    @Override
    public boolean isColumnLevel(GPropertyDraw draw) { return false; }

    // the design value of this option, which the reader's delivered value overrides. These are per-cell options, but their
    // design value is one per column, so it is emitted into the column entry and the row entry overrides it (buildPropMeta).
    // The classic renderers read the same fields off the draw, so a React view now sees exactly what they see.
    @Override
    public String getColumnStatic(GComponent owner) {
        GPropertyDraw draw = (GPropertyDraw) owner;
        if (readerType == COMMENT) return draw.comment;
        if (readerType == PLACEHOLDER) return draw.placeholder;
        if (readerType == PATTERN) return draw.pattern;
        if (readerType == REGEXP) return draw.regexp;
        if (readerType == REGEXPMESSAGE) return draw.regexpMessage;
        if (readerType == TOOLTIP) return draw.tooltip;
        if (readerType == VALUETOOLTIP) return draw.valueTooltip;
        return null; // PROPERTY_CUSTOM_OPTIONS / DEFAULTVALUE have no string design value
    }

    private static String getPrefix(int readerType) {
        if (readerType == CELL_FONT) {
            return "CELL_FONT";
        } else if (readerType == COMMENT) {
            return "COMMENT";
        } else if (readerType == COMMENTELEMENTCLASS) {
            return "COMMENTELEMENTCLASS";
        } else if (readerType == PLACEHOLDER) {
            return "PLACEHOLDER";
        } else if (readerType == PATTERN) {
            return "PATTERN";
        } else if (readerType == REGEXP) {
            return "REGEXP";
        } else if (readerType == REGEXPMESSAGE) {
            return "REGEXPMESSAGE";
        } else if (readerType == TOOLTIP) {
            return "TOOLTIP";
        } else if (readerType == VALUETOOLTIP) {
            return "VALUETOOLTIP";
        } else if (readerType == PROPERTY_CUSTOM_OPTIONS) {
            return "PROPERTY_CUSTOM_OPTIONS";
        } else if (readerType == CHANGEKEY) {
            return "CHANGEKEY";
        } else if (readerType == CHANGEMOUSE) {
            return "CHANGEMOUSE";
        } else if (readerType == CAPTIONELEMENTCLASS) {
            return "CAPTIONELEMENTCLASS";
        } else if (readerType == FOOTERELEMENTCLASS) {
            return "FOOTERELEMENTCLASS";
        }
        return null;
    }
}
