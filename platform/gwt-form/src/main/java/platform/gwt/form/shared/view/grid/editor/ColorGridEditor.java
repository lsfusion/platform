package platform.gwt.form.shared.view.grid.editor;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.*;
import net.auroris.ColorPicker.client.ColorPicker;
import platform.gwt.form.client.MainFrameMessages;
import platform.gwt.form.shared.view.changes.dto.ColorDTO;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.form.shared.view.grid.renderer.ColorGridRenderer;

public class ColorGridEditor extends PopupBasedGridEditor {
    private static final MainFrameMessages messages = MainFrameMessages.Instance.get();

    private ColorPicker colorPicker;

    public ColorGridEditor(EditManager editManager) {
        super(editManager);
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

        VerticalPanel mainPane = new VerticalPanel();
        mainPane.add(colorPicker);
        mainPane.add(bottomPane);
        mainPane.setCellHorizontalAlignment(bottomPane, HasAlignment.ALIGN_RIGHT);

        return mainPane;
    }

    @Override
    public void startEditing(NativeEvent editEvent, Cell.Context context, Element parent, Object oldValue) {
        if (oldValue != null) {
            try {
                colorPicker.setHex(((ColorDTO)oldValue).value);
            } catch (Exception e) {
                throw new IllegalStateException("can't convert string value to color");
            }
        }

        super.startEditing(editEvent, context, parent, oldValue);
    }

    @Override
    public void render(Cell.Context context, Object value, SafeHtmlBuilder sb) {
        sb.append(ColorGridRenderer.Template.Instance.colorbox(value));
    }
}
