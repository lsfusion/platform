package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public class TextGridCellRenderer extends StringBasedGridCellRenderer {
    private final boolean rich;

    public TextGridCellRenderer(GPropertyDraw property, boolean rich) {
        super(property);
        this.rich = rich;
    }

    @Override
    public void renderStaticContent(Element element, GFont font) {
        Style style = element.getStyle();

        style.setProperty("lineHeight", "normal");
        if (!rich) {
            style.setProperty("wordWrap", "break-word");
            style.setWhiteSpace(Style.WhiteSpace.PRE_WRAP);
        }
    }


    @Override
    public void renderDynamic(Element element, GFont font, Object value, boolean isSingle) {
        if (!rich || value == null) {
            super.renderDynamic(element, font, value, isSingle);
        } else {
            element.removeClassName("nullValueString");
            element.getStyle().setWhiteSpace(Style.WhiteSpace.PRE_WRAP);
            element.setInnerHTML(EscapeUtils.sanitizeHtml((String) value));
        }
    }

    @Override
    protected String castToString(Object value) {
        return (String) value;
    }
}