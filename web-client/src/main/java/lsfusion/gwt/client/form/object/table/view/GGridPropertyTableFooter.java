package lsfusion.gwt.client.form.object.table.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableCellElement;
import lsfusion.gwt.client.base.view.grid.Header;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import static lsfusion.gwt.client.base.GwtSharedUtils.nullEquals;

public class GGridPropertyTableFooter extends Header<String> {

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
        renderTD(th);
        prevValue = value;
    }

    @Override
    public void updateDom(TableCellElement th) {
        if (!nullEquals(this.value, prevValue)) {
            renderTD(th);
            prevValue = value;
        }
    }

    public void renderTD(Element th) {
        property.getCellRenderer().render(th, value, table, table);
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