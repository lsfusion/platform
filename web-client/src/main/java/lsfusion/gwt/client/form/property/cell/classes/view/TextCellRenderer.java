package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;

import static lsfusion.gwt.client.view.StyleDefaults.CELL_HORIZONTAL_PADDING;
import static lsfusion.gwt.client.view.StyleDefaults.CELL_VERTICAL_PADDING;

public class TextCellRenderer extends StringBasedCellRenderer {
    private final boolean rich;

    public TextCellRenderer(GPropertyDraw property, boolean rich) {
        super(property);
        this.rich = rich;
    }

    @Override
    public void renderStaticContent(Element element, RenderContext renderContext) {
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