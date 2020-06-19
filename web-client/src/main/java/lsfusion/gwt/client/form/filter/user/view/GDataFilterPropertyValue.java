package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
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
    protected RenderContext getRenderContext() {
        return RenderContext.DEFAULT;
    }

    @Override
    protected void onPaste(String objValue) {
    }

    @Override
    protected void onEditEvent(Event event, Runnable consumed) {
        form.edit(property, getRenderElement(), property.baseType, event, false, null,
                this::getValue, this::setValue, afterCommit, () -> {}, getRenderContext(), getUpdateContext(), down -> {});
    }
}
