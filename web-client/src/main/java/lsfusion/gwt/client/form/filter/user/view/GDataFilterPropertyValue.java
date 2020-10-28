package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.panel.view.ActionOrPropertyValue;

import java.util.function.Consumer;

public class GDataFilterPropertyValue extends ActionOrPropertyValue {

    private final Consumer<Object> afterCommit;

    public GDataFilterPropertyValue(GPropertyDraw property, GFormController form, Consumer<Object> afterCommit) {
        super(property, form);

        this.afterCommit = afterCommit;

        finalizeInit();
    }

    @Override
    protected void onPaste(Object objValue, String stringValue) {
        afterCommit.accept(objValue);
    }

    // it's a hacky hack, however when filter will become docked it will go away
    @Override
    public EventHandler createEventHandler(Event event) {
        return new EventHandler(event) {
            @Override
            public void consume(boolean propagateToNative, boolean propagateToUpper) {
                if(BrowserEvents.KEYDOWN.equals(event.getType())) {
                    int keyCode = event.getKeyCode();
                    if (keyCode == KeyCodes.KEY_ESCAPE || keyCode == KeyCodes.KEY_ENTER)
                        return;
                }

                super.consume(propagateToNative, propagateToUpper);
            }
        };
    }

    // there is some architecture bug in filters so for now will do this hack (later filter should rerender all GDataFilterValue)
    public void changeProperty(GPropertyDraw property) {
        Element renderElement = getRenderElement();

        this.property.getCellRenderer().clearRender(renderElement, this);

        this.property = property;

        setBaseSize(true);

        property.getCellRenderer().renderStatic(renderElement, this);
    }

    @Override
    public boolean isFocusable() {
        return true;
    }

    @Override
    public boolean isSetLastBlurred() {
        return false;
    }

    @Override
    public Object forceSetFocus() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restoreSetFocus(Object forceSetFocus) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void onEditEvent(EventHandler handler) {
        if(property.isFilterChange(handler.event)) {
            handler.consume();
            form.edit(property.baseType, handler.event, false, null, this::setValue, afterCommit, () -> {}, this);
        }
    }
}
