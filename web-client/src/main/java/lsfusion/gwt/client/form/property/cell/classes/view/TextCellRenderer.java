package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;

public class TextCellRenderer extends StringBasedCellRenderer {
    private final boolean rich;

    public TextCellRenderer(GPropertyDraw property, boolean rich) {
        super(property);
        this.rich = rich;
    }

    @Override
    public int getHeightPadding() {
        return getHeightPadding(true);
    }

    @Override
    public void renderStaticContent(Element element, RenderContext renderContext) {
        super.renderStaticContent(element, renderContext);
        if (!rich) {
            element.getStyle().setProperty("wordWrap", "break-word");
        }
    }

    @Override
    protected boolean isMultiLine() {
        return true;
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