package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.*;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.classes.data.GColorType;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.async.GInputListAction;
import lsfusion.gwt.client.form.property.cell.classes.controller.suggest.GCompletionType;
import lsfusion.gwt.client.form.property.cell.controller.CancelReason;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.GUserInputResult;
import lsfusion.gwt.client.form.property.cell.view.RendererType;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;
import lsfusion.gwt.client.form.property.panel.view.ActionOrPropertyValue;
import lsfusion.gwt.client.form.property.panel.view.ActionOrPropertyValueController;
import lsfusion.interop.action.ServerResponse;

import java.util.function.Consumer;

public class GDataFilterPropertyValue extends ActionOrPropertyValue {

    private final GPropertyFilter condition;
    private final Consumer<PValue> afterCommit;
    private final Consumer<CancelReason> onCancel;
    
    private GInputList inputList;
    private GInputListAction[] inputListActions;

    public GDataFilterPropertyValue(GPropertyFilter condition, GFormController form, Consumer<PValue> afterCommit, Consumer<CancelReason> onCancel) {
        super(condition.property, condition.columnKey, form, false, new ActionOrPropertyValueController() {
            @Override
            public void setValue(GGroupObjectValue columnKey, PValue value) {
            }

            @Override
            public void setLoading(GGroupObjectValue columnKey, PValue value) {
                throw new UnsupportedOperationException();
            }
        });
        this.condition = condition;
        this.afterCommit = afterCommit;
        this.onCancel = onCancel;
        
        changeInputList(condition.compare);

        render();
    }

    private void updateValue(PValue value, boolean loading) {
        update(value, loading, null, property.valueElementClass, property.font, property.getBackground(), property.getForeground(), null,
                null, property.getPattern(), property.regexp, property.regexpMessage, property.valueTooltip);
    }

    public void updateValue(PValue value) {
        updateValue(value, loading);
    }

    public void updateLoading(boolean loading) {
        updateValue(value, loading);
    }

    public
    @Override void pasteValue(String stringValue) {
        updateAndCommit(PValue.escapeSeparator(property.parsePaste(stringValue, property.getPasteType()), inputList.compare));
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
        onEditEvent(handler, false);
    }

    protected void onEditEvent(EventHandler handler, boolean forceEdit) {
        Result<Boolean> contextAction = new Result<>();
        if((property.isFilterChange(handler.event, contextAction) || forceEdit) && !property.isCustom(getRendererType())) {
            if(contextAction.result != null) // assert that reset is called
                updateAndCommit(null);
            else
                startEditing(handler);

            if(!handler.consumed)
                handler.consume();
        }
    }

    private PValue getValue(GUserInputResult result) {
        if(result.getContextAction() != null) // assert that reset is called
            return null;
        return result.getPValue();
    }

    protected void startEditing(EventHandler handler) {
        form.edit(property.getFilterBaseType(),
                handler,
                false,
                null,
                inputList,
                inputListActions,
                (result, commitReason) -> setValue(getValue(result)),
                (result, commitReason) -> acceptCommit(getValue(result)),
                onCancel,
                this,
                inputList.completionType.isExactMatchNeeded() ? ServerResponse.STRICTVALUES : ServerResponse.VALUES, null);
    }

    @Override
    public Object modifyPastedString(String pastedText) {
        return GwtClientUtils.escapeSeparator(pastedText, condition.compare);
    }

    private void acceptCommit(PValue result) {
        afterCommit.accept(result);
    }

    @Override
    public void changeProperty(PValue result, GFormController.ChangedRenderValueSupplier renderValueSupplier) {
        if(renderValueSupplier != null)
            result = renderValueSupplier.getValue(getValue(), result);
        updateAndCommit(result);
    }

    private void updateAndCommit(PValue pValue) {
        updateValue(pValue);
        afterCommit.accept(pValue);
    }

    public void setApplied(boolean applied) {
        if (!(property.getFilterBaseType() instanceof GColorType)) {
            if (applied) {
                getElement().addClassName("filter-applied");
            } else {
                getElement().removeClassName("filter-applied");
            }
        }
    }

    public void changeInputList(GCompare compare) {
        inputList = new GInputList(compare == GCompare.EQUALS || compare == GCompare.NOT_EQUALS ? GCompletionType.SEMI_STRICT :
                GCompletionType.NON_STRICT,
                compare);
        inputListActions = new GInputListAction[]{new GInputListAction(StaticImage.RESET, AppStaticImage.INPUT_RESET, null, null, null, null, 0)};
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
        public BaseStaticImage getImage() {
            return StaticImage.RESET;
        }

        @Override
        public void setOnPressed(Element actionImgElement, UpdateContext updateContext) {
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

    @Override
    public RendererType getRendererType() {
        return RendererType.FILTER;
    }

    @Override
    protected GComponent getComponent() {
        return condition.filter;
    }

    @Override
    public boolean isInputRemoveAllPMB() { // filter is closer to the grid in this case
        return true;
    }
}
