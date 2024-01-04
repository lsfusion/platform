package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
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
        if (readerType == COMMENT) {
            controller.updatePropertyComments(this, values);
        } else if (readerType == COMMENTELEMENTCLASS) {
            controller.updateCellCommentElementClasses(this, values);
        } else if (readerType == PLACEHOLDER) {
            controller.updatePlaceholderValues(this, values);
        } else if (readerType == PATTERN) {
            controller.updatePatternValues(this, values);
        } else if (readerType == TOOLTIP) {
            controller.updateTooltipValues(this, values);
        }else if (readerType == VALUETOOLTIP) {
            controller.updateValueTooltipValues(this, values);
        }
    }

    private static String getPrefix(int readerType) {
        if (readerType == COMMENT) {
            return "COMMENT";
        } else if (readerType == COMMENTELEMENTCLASS) {
            return "COMMENTELEMENTCLASS";
        } else if (readerType == PLACEHOLDER) {
            return "PLACEHOLDER";
        } else if (readerType == PATTERN) {
            return "PATTERN";
        } else if (readerType == TOOLTIP) {
            return "TOOLTIP";
        } else if (readerType == VALUETOOLTIP) {
            return "VALUETOOLTIP";
        }
        return null;
    }
}
