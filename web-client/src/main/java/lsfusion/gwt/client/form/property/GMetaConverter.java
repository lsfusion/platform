package lsfusion.gwt.client.form.property;

import com.google.gwt.core.client.JavaScriptObject;
import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.classes.data.GJSONType;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.object.table.grid.view.GSimpleStateTableView;
import lsfusion.gwt.client.form.property.cell.view.RendererType;

// how a presentation reader's value is converted for data.meta / data.components — declared by the reader
// (getMetaConverter) and converted polymorphically here. Returns the JS value to store: a String, Boolean or plain JS object.
public enum GMetaConverter {
    CLASS   { public Object convert(PValue v, GPropertyDraw d) { return PValue.getClassStringValue(v); } },
    COLOR   { public Object convert(PValue v, GPropertyDraw d) { return PValue.getColorStringValue(v); } },
    CAPTION { public Object convert(PValue v, GPropertyDraw d) { return trim(PValue.getCaptionStringValue(v)); } }, // like GGridPropertyTable.getDynamicCaption
    STRING  { public Object convert(PValue v, GPropertyDraw d) { return PValue.getStringValue(v); } },
    TEXT    { public Object convert(PValue v, GPropertyDraw d) { return trim(PValue.getStringValue(v)); } }, // trimmed string, like getDynamicComment / getDynamicTooltip
    IMAGE   { public Object convert(PValue v, GPropertyDraw d) { BaseImage i = PValue.getImageValue(v); return i != null ? i.createImageHTML() : null; } },
    BOOLEAN { public Object convert(PValue v, GPropertyDraw d) { return PValue.get3SBooleanValue(v); } }, // 3-state, like GGridTable.setReadOnly; may be null
    FLAG    { public Object convert(PValue v, GPropertyDraw d) { return PValue.getBooleanValue(v); } }, // 2-state present->true (like row select): NOT get3SBooleanValue (a client-optimistic false is still present -> true)
    FONT    { public Object convert(PValue v, GPropertyDraw d) { GFont f = PValue.getFontValue(v); return f != null ? buildFont(f.family, f.size, f.bold, f.italic) : null; } }, // plain { family, size, bold, italic }
    FOOTER  { public Object convert(PValue v, GPropertyDraw d) { return GSimpleStateTableView.convertToJSValue(d, v, RendererType.SIMPLE, true); } }, // value-typed, like the cell value
    JSON    { public Object convert(PValue v, GPropertyDraw d) { return GSimpleStateTableView.convertToJSValue(GJSONType.instance, null, false, v); } }; // JSON, like the GWT views

    public abstract Object convert(PValue value, GPropertyDraw draw);

    private static String trim(String s) { return s != null ? s.trim() : null; }

    private static native JavaScriptObject buildFont(String family, int size, boolean bold, boolean italic) /*-{ return { family: family, size: size, bold: bold, italic: italic }; }-*/;
}
