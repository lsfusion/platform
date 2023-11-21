package lsfusion.gwt.client.form.object.table.view;

import com.google.gwt.dom.client.TableCellElement;
import lsfusion.gwt.client.base.view.grid.Header;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.RendererType;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;
import lsfusion.gwt.client.form.property.table.view.GPropertyTableBuilder;

import static lsfusion.gwt.client.base.GwtSharedUtils.nullEquals;

public class GGridPropertyTableFooter extends Header<String> implements RenderContext, UpdateContext {

    private GGridPropertyTable table;
    protected GPropertyDraw property;

    protected PValue prevValue;
    protected PValue value;
    private boolean sticky;

    public GGridPropertyTableFooter(GGridPropertyTable table, GPropertyDraw property, PValue value, String tooltip, boolean sticky) {
        this.table = table;
        this.property = property;
        this.value = value;
        this.sticky = sticky;
    }

    public void setValue(PValue value) {
        this.value = value;
    }

    @Override
    public boolean globalCaptionIsDrawn() {
        return true;
    }

    @Override
    public boolean isSelectedRow() {
        return false;
    }

    @Override
    public GFont getFont() {
        return table.getFont();
    }

    @Override
    public PValue getValue() {
        return value;
    }

    @Override
    public void renderAndUpdateDom(TableCellElement th) {
        if (sticky) {
            th.addClassName("dataGridStickyFooter");
            th.addClassName("background-inherit");
        }
        
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

    @Override
    public RendererType getRendererType() {
        return RendererType.FOOTER;
    }
}