package lsfusion.gwt.client.form.object.table.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.TableCellElement;
import lsfusion.gwt.client.base.view.grid.Header;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.table.view.GPropertyTableBuilder;

import static lsfusion.gwt.client.base.GwtSharedUtils.nullEquals;

public class GGridPropertyTableFooter extends Header<String> {

    private Element renderedCaptionElement;

    private GGridPropertyTable table;
    protected GPropertyDraw property;

    protected Object prevValue;
    protected Object value;

    public GGridPropertyTableFooter(GGridPropertyTable table, GPropertyDraw property, Object value, String toolTip) {
        super(table, toolTip);
        this.table = table;
        this.property = property;
        this.value = value;
    }

    public void setValue(Object value) {
        this.value = value;
        this.tooltip = property.getTooltipText(getRenderedCaption());
    }

    @Override
    public void renderDom(TableCellElement th) {
        renderedCaptionElement = renderTD(th);
        prevValue = value;
    }

    public Element renderTD(Element th) {
        Style.TextAlign textAlign = property.getTextAlignStyle();
        if (textAlign != null) {
            th.getStyle().setTextAlign(textAlign);
        }
        Integer height = table.getStaticHeight();
        if (height != null) {
            GPropertyTableBuilder.setLineHeight(th, height);
        }
        renderCaption(th, getRenderedCaption());

        return th;
    }

    @Override
    public void updateDom(TableCellElement th) {
        if (!nullEquals(this.value, prevValue)) {
            renderCaption(renderedCaptionElement, getRenderedCaption());
            prevValue = value;
        }
    }

    private String getRenderedCaption() {
        String result = null;
        if (value != null) {
            try {
                result = property.getCellRenderer().format(value);
            } catch (Exception ignored) {
                result = value.toString();
            }
        }
        return result != null ? result : "";
    }
}