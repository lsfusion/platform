package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.event.GKeyStroke;
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
    // todo 
    @Override
    public EventHandler createEventHandler(Event event) {
        return new EventHandler(event) {
            @Override
            public void consume(boolean propagateToNative, boolean propagateToUpper) {
                if (GKeyStroke.isEnterKeyEvent(event)) {
                    super.consume(false, true);
//                    return;
                } else {
                    super.consume(propagateToNative, propagateToUpper);
                }
            }
        };
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

    @Override
    protected void onBlur(EventHandler handler) {
        form.previewBlurEvent(handler.event);
        
        super.onBlur(handler);
    }
}
