package lsfusion.gwt.form.shared.view.grid.editor.rich;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.InitializeEvent;
import com.google.gwt.event.logical.shared.InitializeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.EscapeUtils;
import lsfusion.gwt.base.client.GwtClientUtils;
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.base.client.ui.GFlexAlignment;
import lsfusion.gwt.cellview.client.cell.Cell;
import lsfusion.gwt.form.client.MainFrameMessages;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.grid.EditEvent;
import lsfusion.gwt.form.shared.view.grid.EditManager;
import lsfusion.gwt.form.shared.view.grid.editor.DialogBasedGridCellEditor;

public class RichTextGridCellEditor extends DialogBasedGridCellEditor {
    private static final MainFrameMessages messages = MainFrameMessages.Instance.get();

    private static int INITIAL_WIDTH = Math.min(800, Window.getClientWidth() - 20);
    private static int INITIAL_HEIGHT = Math.min(600, Window.getClientHeight() - 20);

    private RichTextArea textArea;

    public RichTextGridCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property, property.getNotEmptyCaption(), INITIAL_WIDTH, INITIAL_HEIGHT);
    }

    @Override
    protected Widget createComponent(EditEvent editEvent, Cell.Context context, Element parent, Object oldValue) {
        textArea = new RichTextArea();
        textArea.setHTML(EscapeUtils.sanitizeHtml(oldValue == null ? "" : oldValue.toString()));
        
        Button btnOk = new Button(messages.ok());
        btnOk.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                commit();
            }
        });

        Button btnCancel = new Button(messages.cancel());
        btnCancel.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                cancel();
            }
        });

        RichTextAreaToolbar toolbar = new RichTextAreaToolbar(textArea);
        
        FlexPanel contentsContainer = new FlexPanel(true);
        contentsContainer.addStretched(toolbar);
        contentsContainer.addFill(textArea);

        FlexPanel bottomPane = new FlexPanel(false, FlexPanel.Justify.CENTER);
        bottomPane.getElement().getStyle().setPadding(5, Style.Unit.PX);
        bottomPane.add(btnOk);
        bottomPane.add(GwtClientUtils.createHorizontalStrut(5));
        bottomPane.add(btnCancel);

        final FlexPanel mainPane = new FlexPanel(true);
        mainPane.addFill(contentsContainer, "0");
        mainPane.add(bottomPane, GFlexAlignment.CENTER);

        textArea.addInitializeHandler(new InitializeHandler() {
            @Override
            public void onInitialize(InitializeEvent event) {
                textArea.setFocus(true);
                textArea.getFormatter().selectAll();
            }
        });

        textArea.addKeyDownHandler(new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent event) {
                int keyCode = event.getNativeKeyCode();
                if (keyCode == KeyCodes.KEY_ENTER && event.isControlKeyDown()) {
                    commit();
                } else if (keyCode == KeyCodes.KEY_ESCAPE) {
                    cancel();
                } 
            }
        });
        
        return mainPane;
    }
    
    @Override
    protected void onCloseClick() {
        cancel();
    }

    private void commit() {
        Object currentText = textArea.getHTML();
        commitEditing(currentText);
    }

    private void cancel() {
        cancelEditing();
    }
}
