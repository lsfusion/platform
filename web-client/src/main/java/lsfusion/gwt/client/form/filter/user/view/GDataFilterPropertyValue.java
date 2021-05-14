package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.async.GAsyncExec;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.panel.view.ActionOrPropertyValue;
import lsfusion.interop.action.ServerResponse;

import java.text.ParseException;
import java.util.function.Consumer;

public class GDataFilterPropertyValue extends ActionOrPropertyValue {

    private final Consumer<Object> afterCommit;

    public GDataFilterPropertyValue(GPropertyDraw property, GGroupObjectValue columnKey, GFormController form, Consumer<Object> afterCommit) {
        super(property, columnKey, form);

        this.afterCommit = afterCommit;

        finalizeInit();
    }

    @Override
    public void pasteValue(String stringValue) {
        Object objValue = null;
        try {
            objValue = property.baseType.parseString(stringValue, property.pattern);
        } catch (ParseException ignored) {}
        updateValue(objValue);

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

    public static final GInputList FILTER = new GInputList(new String[0], new GAsyncExec[0], false);

    @Override
    protected void onEditEvent(EventHandler handler) {
        if(property.isFilterChange(handler.event)) {
            handler.consume();
            form.edit(property.baseType, handler.event, false, null, FILTER, result -> setValue(result.getValue()), result -> afterCommit.accept(result.getValue()), () -> {}, this, ServerResponse.FILTER);
        }
    }

    @Override
    public Consumer<Object> getCustomRendererValueChangeConsumer() {
        return value -> {
            updateValue(value);
            afterCommit.accept(value);
        };
    }

    @Override
    public boolean isPropertyReadOnly() {
        return false;
    }

    @Override
    protected void onBlur(EventHandler handler) {
        form.previewBlurEvent(handler.event);

        super.onBlur(handler);
    }
}
