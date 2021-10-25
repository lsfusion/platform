package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.WindowBox;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.WindowValueCellEditor;

public abstract class DialogBasedCellEditor extends WindowValueCellEditor {
    protected final GPropertyDraw property;

    protected WindowBox dialog;

    protected final String title;
    protected final int width;
    protected final int height;

    public DialogBasedCellEditor(EditManager editManager, GPropertyDraw property, String title, int width, int height) {
        super(editManager);
        this.property = property;

        this.title = title;
        this.width = width;
        this.height = height;
    }

    @Override
    public void start(Event editEvent, Element parent, Object oldValue) {
        dialog = new WindowBox(false, true, true) {
            @Override
            protected void onCloseClick(ClickEvent event) {
                cancel(parent);
            }
        };
        dialog.setText(title);
        dialog.setModal(true);
        dialog.setGlassEnabled(true);

        Widget content = createComponent(parent, oldValue);
        if (width != -1 && height != -1) {
            content.setPixelSize(width, height);
        }
        dialog.setWidget(content);
        dialog.center();
    }

    @Override
    public void stop(Element parent, boolean cancel) {
        dialog.hide();
    }

    protected abstract Widget createComponent(Element parent, Object oldValue);
}
