package platform.gwt.form.shared.view.grid.editor;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.*;
import net.auroris.ColorPicker.client.ColorPicker;
import platform.gwt.cellview.client.cell.Cell;
import platform.gwt.form.client.MainFrameMessages;
import platform.gwt.form.shared.view.changes.dto.ColorDTO;
import platform.gwt.form.shared.view.grid.EditEvent;
import platform.gwt.form.shared.view.grid.EditManager;

public class ColorGridCellEditor extends PopupBasedGridCellEditor {
    private static final MainFrameMessages messages = MainFrameMessages.Instance.get();

    public interface Template extends SafeHtmlTemplates {
        @Template("<div style=\"height: 16px; border: 0px solid black; background: {0}; color: {0};\">&nbsp</div>")
        SafeHtml colorbox(String color);

        public static final class Instance {
            private static final ColorGridCellEditor.Template template = GWT.create(ColorGridCellEditor.Template.class);

            public static ColorGridCellEditor.Template get() {
                return template;
            }

            public static SafeHtml colorbox(Object value) {
                return get().colorbox(value == null ? "" : value.toString());
            }
        }
    }

    private ColorPicker colorPicker;

    public ColorGridCellEditor(EditManager editManager) {
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

    @Override
    protected void render(Cell.Context context, Object value, SafeHtmlBuilder sb) {
        sb.append(Template.Instance.colorbox(value));
    }
}
