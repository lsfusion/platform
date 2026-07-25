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
    public String getAttributeField() {
        if (readerType == COMMENT) return "comment";
        if (readerType == PLACEHOLDER) return "placeholder";
        if (readerType == PATTERN) return "pattern";
        if (readerType == REGEXP) return "regexp";
        if (readerType == REGEXPMESSAGE) return "regexpMessage";
        if (readerType == TOOLTIP) return "tooltip";
        if (readerType == VALUETOOLTIP) return "valueTooltip";
        if (readerType == PROPERTY_CUSTOM_OPTIONS) return "options";
        if (readerType == DEFAULTVALUE) return "defaultValue";
        return null; // CHANGEKEY/CHANGEMOUSE/... are not projected
    }

    @Override
    public GAttributeConverter getAttributeConverter() {
        if (readerType == COMMENT || readerType == TOOLTIP) return GAttributeConverter.TEXT; // both are a trimmed string
        if (readerType == PROPERTY_CUSTOM_OPTIONS) return GAttributeConverter.JSON;
        return GAttributeConverter.STRING; // PLACEHOLDER/PATTERN/REGEXP/REGEXPMESSAGE/VALUETOOLTIP/DEFAULTVALUE
    }

    // which axis the SERVER reads this reader over, mirroring FormInstance.getChangedDrawProps: the readers below are
    // filled over propRowColumnGrids (the column groups only, so a single EMPTY key when there are none), the rest over
    // propRowGrids (which includes toDraw for a list draw, i.e. one value per row). That axis is what splits a list
    // property's projection: a column-axis reader is emitted once in node.<prop> (GReactFormData.buildColumnEntry) and a
    // row-axis one per cell (buildCellEntry), and the delta path dirties the node or the rows to match.
    // The answer is stated for every reader type, including ones the projection does not carry today (element classes,
    // changeKey/changeMouse): it describes the server's axis, not whether this branch happens to read it.
    @Override
    public boolean isColumnAttribute(GPropertyDraw draw) {
        return readerType == COMMENT || readerType == COMMENTELEMENTCLASS || readerType == TOOLTIP
                || readerType == CHANGEKEY || readerType == CHANGEMOUSE
                || readerType == CAPTIONELEMENTCLASS || readerType == FOOTERELEMENTCLASS
                || readerType == DEFAULTVALUE;
    }

    // the design value of this option, which the reader's delivered value overrides. These are per-cell options, but their
    // design value is one per column, but it is the FALLBACK of a per-cell attribute, so it is emitted in each CELL entry
    // (data.<group>.list[i].<prop>) as that cell's effective value - never separately at the column, so nothing is merged.
    // The classic renderers read the same fields off the draw, so a React view now sees exactly what they see.
    @Override
    public String getStaticAttribute(GComponent owner) {
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
        } else if (readerType == DEFAULTVALUE) {
            return "DEFAULTVALUE";
        }
        // the prefix makes the reader's SID, and that SID identifies its values (the panel keeps them in a map keyed by
        // reader), so a type with no name of its own must still get one that no other type can take
        return "TYPE" + readerType;
    }
}
