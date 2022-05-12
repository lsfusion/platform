package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.Result;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.async.GInputListAction;
import lsfusion.gwt.client.form.property.cell.classes.controller.suggest.GCompletionType;
import lsfusion.gwt.client.form.property.cell.controller.CancelReason;
import lsfusion.gwt.client.form.property.cell.controller.CommitReason;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.GUserInputResult;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;
import lsfusion.gwt.client.form.property.panel.view.ActionOrPropertyValue;
import lsfusion.gwt.client.form.property.panel.view.ActionOrPropertyValueController;
import lsfusion.interop.action.ServerResponse;

import java.text.ParseException;
import java.util.function.Consumer;

public class GDataFilterPropertyValue extends ActionOrPropertyValue {

    private final Consumer<Object> afterCommit;
    private final Consumer<CancelReason> onCancel;
    
    private GInputList inputList;
    
    public boolean enterPressed;

    public GDataFilterPropertyValue(GPropertyFilter condition, GFormController form, Consumer<Object> afterCommit, Consumer<CancelReason> onCancel) {
        super(condition.property, condition.columnKey, form, false, new ActionOrPropertyValueController() {
            @Override
            public void setValue(GGroupObjectValue columnKey, Object value) {
            }

            @Override
            public void setLoading(GGroupObjectValue columnKey, Object value) {
                throw new UnsupportedOperationException();
            }
        });
        this.afterCommit = afterCommit;
        this.onCancel = onCancel;
        
        changeInputList(condition.compare);

        finalizeInit();
    }

    public void updateValue(Object value) {
        update(value, loading, null, null, null, false);
    }

    public void updateLoading(boolean loading) {
        update(value, loading, null, null, null, false);
    }

    @Override
    public void pasteValue(String stringValue) {
        updateAndCommit(property.parsePaste(stringValue, property.baseType));
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
        Result<Boolean> contextAction = new Result<>();
        if(property.isFilterChange(handler.event, contextAction)) {
            handler.consume();
            if(contextAction.result != null) // assert that reset is called
                updateAndCommit(null);
            else
                startEditing(handler.event);
        }
    }

    private Object getValue(GUserInputResult result) {
        if(result.getContextAction() != null) // assert that reset is called
            return null;
        return result.getValue();
    }

    protected void startEditing(Event event) {
        form.edit(property.getFilterBaseType(),
                event,
                false,
                null,
                inputList,
                (result, commitReason) -> setValue(getValue(result)),
                (result, commitReason) -> acceptCommit(getValue(result), commitReason.equals(CommitReason.ENTERPRESSED)),
                onCancel,
                this,
                ServerResponse.VALUES, null);
    }

    private void acceptCommit(Object result, boolean enterPressed) {
        this.enterPressed = enterPressed;
        afterCommit.accept(result);
        this.enterPressed = false;
    }

    @Override
    public void changeProperty(Object result) {
        updateAndCommit(result);
    }

    @Override
    public void executeContextAction(int action) {
        throw new UnsupportedOperationException();
    }

    private void updateAndCommit(Object value) {
        updateValue(value);
        afterCommit.accept(value);
    }

    public void setApplied(boolean applied) {
        if (applied) {
            getElement().addClassName("userFilerValueCellApplied");
        } else {
            getElement().removeClassName("userFilerValueCellApplied");
        }
    }

    public void changeInputList(GCompare compare) {
        inputList = new GInputList(new GInputListAction[]{new GInputListAction("reset", null, null, null, null)},
                compare == GCompare.EQUALS || compare == GCompare.NOT_EQUALS ? GCompletionType.SEMI_STRICT : GCompletionType.NON_STRICT);
    }

    private static final CellRenderer.ToolbarAction dropAction = new CellRenderer.ToolbarAction() {
        @Override
        public boolean isHover() {
            return true;
        }

        @Override
        public GKeyStroke getKeyStroke() {
            return null;
        }

        @Override
        public String getImage() {
            return "reset";
        }

        @Override
        public void setOnPressed(ImageElement actionImgElement, UpdateContext updateContext) {
            setToolbarAction(actionImgElement, true);
//            setToolbarAction(actionImgElement, () -> updateContext.changeProperty(null));
        }

        @Override
        public boolean matches(CellRenderer.ToolbarAction action) {
            return false;
        }
    };
    private static final CellRenderer.ToolbarAction[] filterActions = new CellRenderer.ToolbarAction[] {dropAction};

    @Override
    public CellRenderer.ToolbarAction[] getToolbarActions() {
        return filterActions;
    }

    @Override
    public boolean canUseChangeValueForRendering(GType type) {
        return true;
    }
}
