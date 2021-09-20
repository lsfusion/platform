package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.view.ResizableVerticalPanel;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.ColorDTO;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import net.auroris.ColorPicker.client.ColorPicker;

public class ColorCellEditor extends PopupBasedCellEditor {
    private static final ClientMessages messages = ClientMessages.Instance.get();

    private ColorPicker colorPicker;

    public ColorCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property);
    }

    @Override
    protected Widget createPopupComponent(Element parent) {
        colorPicker = new ColorPicker();

        Button btnOk = new Button(messages.ok());
        btnOk.addClickHandler(event -> validateAndCommit(parent, false));

        Button btnCancel = new Button(messages.cancel());
        btnCancel.addClickHandler(event -> cancel(parent));

        Button btnReset = new Button(messages.reset());
        btnReset.addClickHandler(event -> reset(parent));

        FlowPanel bottomPane = new FlowPanel();
        bottomPane.add(btnOk);
        bottomPane.add(btnCancel);
        bottomPane.add(btnReset);

        ResizableVerticalPanel mainPane = new ResizableVerticalPanel();
        mainPane.add(colorPicker);
        mainPane.add(bottomPane);
        mainPane.setCellHorizontalAlignment(bottomPane, HasAlignment.ALIGN_RIGHT);

        return mainPane;
    }

    @Override
    public Object getValue(Element parent, Integer contextAction) {
        return new ColorDTO(colorPicker.getHexColor());
    }

    public void reset(Element parent) {
        commitValue(parent, (ColorDTO) null);
    }

    @Override
    public void start(Event editEvent, Element parent, Object oldValue) {
        super.start(editEvent, parent, oldValue);

        if (oldValue instanceof ColorDTO) {
            try {
                colorPicker.setHex(((ColorDTO)oldValue).value);
            } catch (Exception e) {
                throw new IllegalStateException("can't convert string value to color");
            }
        }
    }
}
