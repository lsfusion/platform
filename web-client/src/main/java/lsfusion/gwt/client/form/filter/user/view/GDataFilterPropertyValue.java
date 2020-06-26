package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.view.EventHandler;
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

    // it's a hacky hack, however when filter will become docked it will go away
    @Override
    public EventHandler createEventHandler(Event event) {
        return new EventHandler(event) {
            @Override
            public void consume() {
                if(BrowserEvents.KEYDOWN.equals(event.getType())) {
                    int keyCode = event.getKeyCode();
                    if (keyCode == KeyCodes.KEY_ESCAPE || keyCode == KeyCodes.KEY_ENTER)
                        return;
                }
                super.consume();
            }
        };
    }

    @Override
    protected void onEditEvent(EventHandler handler) {
        if(property.isFilterChange(handler.event)) {
            handler.consume();
            form.edit(property, getRenderElement(), property.baseType, handler.event, false, null,
                    this::getValue, this::setValue, afterCommit, () -> {
                    }, getRenderContext(), getUpdateContext());
        }
    }
}
