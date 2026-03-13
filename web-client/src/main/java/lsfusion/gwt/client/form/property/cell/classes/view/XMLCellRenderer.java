package lsfusion.gwt.client.form.property.cell.classes.view;

import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.RendererType;

public class XMLCellRenderer extends TextCellRenderer {

    public XMLCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    public String format(PValue value, RendererType rendererType, String pattern) {
        String stringValue = super.format(value, rendererType, pattern);
        return formatXML(stringValue);
    }

    private static native String formatXML(String value)/*-{
        if (value == null || value === '')
            return value;

        var formatted = "";
        var pad = 0;

        value = value.replace(/(>)(<)(\/*)/g, "$1\n$2$3");
        var lines = value.split("\n");

        for (var i = 0; i < lines.length; i++) {
            var line = lines[i];
            var indent = 0;

            if (line.match(/.+<\/\w[^>]*>$/)) {
                indent = 0;
            } else if (line.match(/^<\/\w/)) {
                if (pad > 0) pad -= 1;
            } else if (line.match(/^<\w[^>]*[^\/]>.*$/)) {
                indent = 1;
            }

            formatted += "  ".repeat(pad) + line + "\n";
            pad += indent;
        }

        return formatted.trim();
    }-*/;
}
