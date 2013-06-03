package lsfusion.gwt.form.shared.view.grid.editor;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Widget;
import net.auroris.ColorPicker.client.ColorPicker;
import lsfusion.gwt.base.client.ui.ResizableVerticalPanel;
import lsfusion.gwt.cellview.client.cell.Cell;
import lsfusion.gwt.form.client.MainFrameMessages;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.changes.dto.ColorDTO;
import lsfusion.gwt.form.shared.view.grid.EditEvent;
import lsfusion.gwt.form.shared.view.grid.EditManager;

public class ColorGridCellEditor extends PopupBasedGridCellEditor {
    private static final MainFrameMessages messages = MainFrameMessages.Instance.get();

    private ColorPicker colorPicker;

    public ColorGridCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property);
    }

    @Override
    protected Widget createPopupComponent() {
        colorPicker = new ColorPicker();

        Button btnOk = new Button(messages.ok());
        btnOk.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                commitEditing(new ColorDTO(colorPicker.getHexColor()));
            }
        });

        Button btnCancel = new Button(messages.cancel());
        btnCancel.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                cancelEditing();
            }
        });

        FlowPanel bottomPane = new FlowPanel();
        bottomPane.add(btnOk);
        bottomPane.add(btnCancel);

        ResizableVerticalPanel mainPane = new ResizableVerticalPanel();
        mainPane.add(colorPicker);
        mainPane.add(bottomPane);
        mainPane.setCellHorizontalAlignment(bottomPane, HasAlignment.ALIGN_RIGHT);

        return mainPane;
    }

    @Override
    public void startEditing(EditEvent editEvent, Cell.Context context, Element parent, Object oldValue) {
        if (oldValue != null) {
            try {
                colorPicker.setHex(((ColorDTO)oldValue).value);
            } catch (Exception e) {
                throw new IllegalStateException("can't convert string value to color");
            }
        }

        super.startEditing(editEvent, context, parent, oldValue);
    }
}
