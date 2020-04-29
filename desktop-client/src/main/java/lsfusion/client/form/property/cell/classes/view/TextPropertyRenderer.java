package lsfusion.client.form.property.cell.classes.view;

import lsfusion.client.base.SwingUtils;
import lsfusion.client.controller.MainController;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.rich.RichEditorKit;
import lsfusion.client.form.property.cell.classes.controller.rich.RichEditorPane;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.view.MainFrame;

import javax.swing.*;
import java.awt.*;


public class TextPropertyRenderer extends PropertyRenderer {
    private boolean rich;
    private JEditorPane pane;
    private boolean isEditableNotNull;

    public TextPropertyRenderer(ClientPropertyDraw property, boolean rich) {
        super(property);
        this.rich = rich;

        getComponent().setOpaque(true);
        getComponent().setFont(new Font("Tahoma", Font.PLAIN, MainFrame.getIntUISize(10)));
        getComponent().setEditable(false);
        getComponent().setEditorKitForContentType("text/html", new RichEditorKit());
    }

    public JEditorPane getComponent() {
        if (pane == null) {
            pane = new JEditorPane() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    paintTextComponent(g);
                }
            };
        }
        return pane;
    }

    private void paintTextComponent(Graphics g) {
        if(isEditableNotNull && !MainController.showNotDefinedStrings) {
            SwingUtils.drawHorizontalLine((Graphics2D) g, Color.RED, 0, getComponent().getWidth(), getComponent().getHeight() - 5);
        }
    }

    @Override
    protected boolean showRequiredString() {
        return true;
    }

    @Override
    protected boolean showNotDefinedString() {
        return true;
    }

    public void setValue(Object value) {
        super.setValue(value != null && value.toString().isEmpty() && !MainController.showNotDefinedStrings ? null : value);
        isEditableNotNull = value == null && property != null && property.isEditableNotNull();
        if (value == null) {
            getComponent().setContentType("text");
            if (isEditableNotNull) {
                getComponent().setText(getRequiredStringValue());
            } else {
                getComponent().setText(MainController.showNotDefinedStrings ? NOT_DEFINED_STRING : "");
            }
        } else {
            String text = value.toString().isEmpty() && !MainController.showNotDefinedStrings ? EMPTY_STRING : value.toString();
            if (rich) {
                getComponent().setContentType("text/html");
                RichEditorPane.setText(getComponent(), text);
            } else {
                getComponent().setContentType("text");
                getComponent().setText(text);
            }
        }
    }
}
