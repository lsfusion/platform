package lsfusion.gwt.client.form.object.table.view;

import com.google.gwt.dom.client.TableCellElement;
import lsfusion.gwt.client.base.view.grid.Header;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;
import lsfusion.gwt.client.form.property.table.view.GPropertyTableBuilder;

import java.util.function.Consumer;

import static lsfusion.gwt.client.base.GwtSharedUtils.nullEquals;

public class GGridPropertyTableFooter extends Header<String> implements RenderContext, UpdateContext {

    private GGridPropertyTable table;
    protected GPropertyDraw property;

    protected Object prevValue;
    protected Object value;

    public GGridPropertyTableFooter(GGridPropertyTable table, GPropertyDraw property, Object value, String toolTip) {
        this.table = table;
        this.property = property;
        this.value = value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public boolean isAlwaysSelected() {
        return false;
    }

    @Override
    public boolean globalCaptionIsDrawn() {
        return true;
    }

    @Override
    public boolean isLoading() {
        return false;
    }

    @Override
    public Object getImage() {
        return null;
    }

    @Override
    public GFont getFont() {
        return table.getFont();
    }

    @Override
    public Consumer<Object> getCustomRendererValueChangeConsumer() {
        return null;
    }

    @Override
    public boolean isPropertyReadOnly() {
        return false;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public void renderAndUpdateDom(TableCellElement th) {
        GPropertyTableBuilder.renderAndUpdate(property, th, this, this);
        prevValue = value;
    }

    @Override
    public void updateDom(TableCellElement th) {
        if (!nullEquals(this.value, prevValue)) {
            GPropertyTableBuilder.update(property, th, this);
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