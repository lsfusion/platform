package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.FormButton;
import lsfusion.gwt.client.base.view.ResizableVerticalPanel;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.classes.ColorDTO;
import lsfusion.gwt.client.form.property.cell.controller.CommitReason;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import net.auroris.ColorPicker.client.ColorPicker;

public class ColorCellEditor extends PopupValueCellEditor {
    private static final ClientMessages messages = ClientMessages.Instance.get();

    private ColorPicker colorPicker;

    public ColorCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property);
    }

    @Override
    protected Widget createPopupComponent(Element parent) {
        colorPicker = new ColorPicker();

        FormButton btnOk = new FormButton(messages.ok(), event -> commit(parent, CommitReason.FORCED));

        FormButton btnCancel = new FormButton(messages.cancel(), event -> cancel(parent));

        FormButton btnReset = new FormButton(messages.reset(), event -> reset(parent));

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
    public void enterPressed(Element parent) {
        super.enterPressed(parent);
        validateAndCommit(parent, false, CommitReason.ENTERPRESSED);
    }

    @Override
    public PValue getCommitValue(Element parent, Integer contextAction) {
        return PValue.getPValue(new ColorDTO(colorPicker.getHexColor()));
    }

    public void reset(Element parent) {
        commitValue(parent, (PValue) null);
    }

    @Override
    public void start(EventHandler handler, Element parent, PValue oldValue) {
        super.start(handler, parent, oldValue);

        if (oldValue != null) {
            try {
                colorPicker.setHex(PValue.getColorValue(oldValue).value);
            } catch (Exception e) {
                throw new IllegalStateException("can't convert string value to color");
            }
        }
    }
}
