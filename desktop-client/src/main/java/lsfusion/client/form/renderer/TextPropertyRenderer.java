package lsfusion.client.form.renderer;

import lsfusion.client.Main;
import lsfusion.client.form.PropertyRenderer;
import lsfusion.client.form.editor.rich.RichEditorKit;
import lsfusion.client.form.editor.rich.RichEditorPane;
import lsfusion.client.logics.ClientPropertyDraw;

import javax.swing.*;
import java.awt.*;

import static javax.swing.BorderFactory.createCompoundBorder;
import static javax.swing.BorderFactory.createEmptyBorder;
import static lsfusion.client.form.ClientFormController.colorPreferences;


public class TextPropertyRenderer extends PropertyRenderer {
    private Color defaultForeground;

    private boolean rich;
    private JEditorPane pane;

    public TextPropertyRenderer(ClientPropertyDraw property, boolean rich) {
        super(property);
        this.rich = rich;

        getComponent().setOpaque(true);
        getComponent().setFont(new Font("Tahoma", Font.PLAIN, Main.getIntUIFontSize(10)));
        getComponent().setEditable(false);
        getComponent().setEditorKitForContentType("text/html", new RichEditorKit());

        defaultForeground = getComponent().getForeground();
    }

    public JEditorPane getComponent() {
        if (pane == null) {
            pane = new JEditorPane();
        }
        return pane;
    }

    @Override
    protected void drawForeground(Color conditionalForeground) {
        if (value == null) {
            if (property != null && property.isEditableNotNull()) {
                getComponent().setForeground(REQUIRED_FOREGROUND);
            } else {
                getComponent().setForeground(INACTIVE_FOREGROUND);
            }
        } else {
            getComponent().setForeground(conditionalForeground != null ? conditionalForeground : defaultForeground);
        }
    }

    protected void drawBorder(boolean isInFocusedRow, boolean hasFocus) {
        if (hasFocus) {
            getComponent().setBorder(createCompoundBorder(colorPreferences.getFocusedCellBorder(), createEmptyBorder(1, 2, 0, 1)));
        } else if (isInFocusedRow) {
            getComponent().setBorder(createCompoundBorder(colorPreferences.getSelectedRowBorder(), createEmptyBorder(1, 3, 0, 2)));
        } else {
            getComponent().setBorder(createEmptyBorder(2, 3, 1, 2));
        }
    }

    public void setValue(Object value) {
        super.setValue(value);
        if (value == null) {
            getComponent().setContentType("text");
            if (property != null && property.isEditableNotNull()) {
                getComponent().setText(REQUIRED_STRING);
            } else {
                getComponent().setText(EMPTY_STRING);
            }
        } else {
            if (rich) {
                getComponent().setContentType("text/html");
                RichEditorPane.setText(getComponent(), value.toString());
            } else {
                getComponent().setContentType("text");
                getComponent().setText(value.toString());
            }
        }
    }
}
