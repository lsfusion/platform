package lsfusion.gwt.client.form.object.table.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.grid.Header;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.RendererType;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;
import lsfusion.gwt.client.form.property.table.view.GPropertyTableBuilder;

import static lsfusion.gwt.client.base.GwtSharedUtils.nullEquals;

public class GGridPropertyTableFooter extends Header<String> implements RenderContext, UpdateContext {

    private final GGridPropertyTable table;
    protected final GPropertyDraw property;

    protected PValue prevValue;
    protected PValue value;
    private final boolean sticky;

    private final GFormController form;

    public GGridPropertyTableFooter(GGridPropertyTable table, GPropertyDraw property, PValue value, String tooltip, boolean sticky, GFormController form) {
        this.table = table;
        this.property = property;
        this.value = value;
        this.sticky = sticky;
        this.form = form;
    }

    @Override
    public GFormController getForm() {
        return form;
    }

    @Override
    public Element getDropdownParent(Element element) {
        return table.getDropdownParent(element);
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

    @Override
    public boolean isInputRemoveAllPMB() {
        return true;
    }

    @Override
    public String getPattern() {
        return property.getPattern();
    }

    @Override
    public String getRegexp() {
        return property.regexp;
    }

    @Override
    public Widget getPopupOwnerWidget() {
        return table.getPopupOwnerWidget();
    }

    @Override
    public String getRegexpMessage() {
        return property.regexpMessage;
    }
}