package lsfusion.gwt.client.form.property.panel.view;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.SimplePanel;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.CopyPasteUtils;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.base.view.grid.CellBasedWidgetImpl;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;

import static com.google.gwt.dom.client.BrowserEvents.*;
import static lsfusion.gwt.client.base.GwtClientUtils.stopPropagation;
import static lsfusion.gwt.client.base.view.ColorUtils.getDisplayColor;

// property value renderer with editing
public abstract class ActionOrPropertyValue extends FocusWidget {

    private Object value;

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    protected GPropertyDraw property;

    protected GFormController form;

    public ActionOrPropertyValue(GPropertyDraw property, GFormController form) {
        setElement(Document.get().createDivElement());

//        sinkEvents(Event.ONPASTE);
// , CLICK, KEYDOWN, KEYPRESS, BLUR
        DataGrid.initSinkEvents(this);

        this.property = property;

        this.form = form;

        getRenderElement().setPropertyObject("groupObject", property.groupObject);
    }

    public Element getRenderElement() {
        return getElement();
    }

    protected void finalizeInit() {
        this.form.render(this.property, getRenderElement(), getRenderContext());
    }

    public void addFill(FlexPanel panel) {
        panel.add(this, panel.getWidgetCount(), GFlexAlignment.STRETCH, 1, property.getValueWidth(null), property.getValueHeight(null));
    }

    public void addSimple(SimplePanel panel) {
        panel.add(this);
        setSimpleSize();
    }

    private void setSimpleSize() {
        setWidth(property.getValueWidth(null) + "px");
        setHeight(property.getValueHeight(null) + "px");
    }

    // there is some architecture bug in filters so for now will do this hack (later filter should rerender all GDataFilterValue)
    public void changeProperty(GPropertyDraw property) {
        this.property = property;

        setSimpleSize(); // assert was added with addSimple

        form.rerender(this.property, getRenderElement(), getRenderContext());
    }

    protected boolean isFocusable() {
        return true;
    }

    @Override
    public void onBrowserEvent(Event event) {
//        if ((BrowserEvents.CLICK.equals(event.getType()) || GKeyStroke.isCommonEditKeyEvent(event) &&
//                !event.getCtrlKey() && !event.getAltKey() && !event.getMetaKey()) &&
//                cellEditor == null &&
//                event.getKeyCode() != KeyCodes.KEY_ESCAPE &&
//                event.getKeyCode() != KeyCodes.KEY_ENTER) {
//            startEditing(new NativeEditEvent(event));
//            stopPropagation(event);
//        }

        if(!DataGrid.checkSinkEvents(event))
            return;

        if(BrowserEvents.FOCUS.equals(event.getType())) {
            if(isFocusable())
                addStyleName("dataPanelRendererGridPanelFocused");
        } else if(BrowserEvents.BLUR.equals(event.getType())) {
            if(isFocusable())
                removeStyleName("dataPanelRendererGridPanelFocused");
        }

        Runnable consumed = () -> stopPropagation(event);
        form.onPropertyBrowserEvent(event, getRenderElement(),
            () -> onEditEvent(event, consumed),
            () -> CopyPasteUtils.putIntoClipboard(getRenderElement()),
            () -> executePaste(event),
            consumed);
    }

    private void executePaste(Event event) {
        String line = CopyPasteUtils.getClipboardData(event).trim();
        if (!line.isEmpty()) {
            stopPropagation(event);
            line = line.replaceAll("\r\n", "\n");    // браузеры заменяют разделители строк на "\r\n"
            pasteValue(line);
        }
    }

    protected abstract void onEditEvent(Event event, Runnable consumed);

    protected abstract RenderContext getRenderContext();

    protected UpdateContext getUpdateContext() {
        return UpdateContext.DEFAULT;
    }

    protected abstract void onPaste(String objValue);

    public void pasteValue(final String value) {
        Scheduler.get().scheduleDeferred(() -> {
            Object objValue = null;
            try {
                objValue = property.baseType.parseString(value, property.pattern);
            } catch (ParseException ignored) {}
            updateValue(objValue);

            onPaste(value);
        });
    }

    public void updateValue(Object value) {
        setValue(value);

        form.update(property, getRenderElement(), getValue(), getUpdateContext());
    }

    public void setBackground(String color) {
        GFormController.setBackgroundColor(getRenderElement(), getDisplayColor(color));
    }

    public void setForeground(String color) {
        GFormController.setForegroundColor(getRenderElement(), getDisplayColor(color));
    }
}
