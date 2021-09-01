package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.async.GAsyncExec;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.cell.view.GUserInputResult;
import lsfusion.gwt.client.form.property.panel.view.ActionOrPropertyValue;
import lsfusion.interop.action.ServerResponse;

import java.text.ParseException;
import java.util.function.Consumer;

public class GDataFilterPropertyValue extends ActionOrPropertyValue {

    private final Consumer<Object> afterCommit;
    private final Runnable onCancel;
    
    public boolean enterPressed;

    public GDataFilterPropertyValue(GPropertyDraw property, GGroupObjectValue columnKey, GFormController form, Consumer<Object> afterCommit, Runnable onCancel) {
        super(property, columnKey, form, false, (columnKeyValue, value) -> {});

        this.afterCommit = afterCommit;
        this.onCancel = onCancel;

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
            startEditing(handler.event);
        }
    }
    
    protected void startEditing(Event event) {
        form.edit(property.baseType,
                event,
                false,
                null,
                FILTER,
                result -> setValue(result.getValue()),
                this::acceptCommit,
                onCancel,
                this,
                ServerResponse.FILTER);
    }
    
    private void acceptCommit(GUserInputResult result) {
        enterPressed = result.isEnterPressed();
        afterCommit.accept(result.getValue());
        enterPressed = false;
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
