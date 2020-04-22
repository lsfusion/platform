package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import static lsfusion.gwt.client.view.StyleDefaults.CELL_HORIZONTAL_PADDING;
import static lsfusion.gwt.client.view.StyleDefaults.CELL_VERTICAL_PADDING;

public class TextGridCellRenderer extends StringBasedGridCellRenderer {
    private final boolean rich;

    public TextGridCellRenderer(GPropertyDraw property, boolean rich) {
        super(property);
        this.rich = rich;
    }

    @Override
    public void renderStaticContent(Element element, GFont font) {
        Style style = element.getStyle();
        style.setPaddingTop(CELL_VERTICAL_PADDING, Style.Unit.PX);
        style.setPaddingRight(CELL_HORIZONTAL_PADDING, Style.Unit.PX);
        style.setPaddingBottom(0, Style.Unit.PX);
        style.setPaddingLeft(CELL_HORIZONTAL_PADDING, Style.Unit.PX);

        style.setProperty("lineHeight", "normal");
        if (!rich) {
            style.setProperty("wordWrap", "break-word");
        }
        element.getStyle().setWhiteSpace(Style.WhiteSpace.PRE_WRAP);
    }

    @Override
    protected void setInnerContent(Element element, String innerText) {
        if (rich) {
            element.setInnerHTML(EscapeUtils.sanitizeHtml(innerText));
        } else {
            super.setInnerContent(element, innerText);
        }
    }

    @Override
    public String format(Object value) {
        return (String) value;
    }
}