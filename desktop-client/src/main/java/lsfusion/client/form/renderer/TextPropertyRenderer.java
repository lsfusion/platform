package lsfusion.client.form.renderer;

import lsfusion.client.form.PropertyRenderer;
import lsfusion.client.form.editor.rich.RichEditorKit;
import lsfusion.client.form.editor.rich.RichEditorPane;
import lsfusion.client.logics.ClientPropertyDraw;

import javax.swing.*;
import java.awt.*;


public class TextPropertyRenderer extends JEditorPane implements PropertyRenderer {

    private final boolean rich;

    private Color defaultBackground;
    private ClientPropertyDraw property;

    public TextPropertyRenderer(ClientPropertyDraw property, boolean rich) {
        this.rich = rich;
        this.property = property;

        setOpaque(true);
        setFont(new Font("Tahoma", Font.PLAIN, 10));
        setEditable(false);
        setEditorKitForContentType("text/html", new RichEditorKit());

        if (property.design != null) {
            property.design.designCell(this);
        }
        defaultBackground = getBackground();
    }

    void setSelected(boolean isSelected, boolean hasFocus) {
        if (isSelected && property != null) {
            if (hasFocus) {
                setBorder(BorderFactory.createCompoundBorder(property.colorPreferences.getFocusedCellBorder(), BorderFactory.createEmptyBorder(1, 2, 0, 1)));
                setBackground(property.colorPreferences.getFocusedCellBackground());
            } else {
                setBorder(BorderFactory.createCompoundBorder(property.colorPreferences.getSelectedRowBorder(), BorderFactory.createEmptyBorder(1, 3, 0, 2)));
                setBackground(property.colorPreferences.getSelectedRowBackground());
            }
        } else {
            setBorder(BorderFactory.createEmptyBorder(2, 3, 1, 2));
            setBackground(defaultBackground);
        }
    }

    public JComponent getComponent() {
        return this;
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value == null) {
            setContentType("text");
            if (property.isEditableNotNull()) {
                setText(REQUIRED_STRING);
                setForeground(REQUIRED_FOREGROUND);
            } else {
                setText(EMPTY_STRING);
                setForeground(UIManager.getColor("TextField.inactiveForeground"));
            }
        } else {
            if (rich) {
                setContentType("text/html");
                RichEditorPane.setText(this, value.toString());
            } else {
                setContentType("text");
                setText(value.toString());
            }
            setForeground(UIManager.getColor("TextField.foreground"));
        }
        setSelected(isSelected, hasFocus);
    }

    @Override
    public void paintAsSelected() {
        if (property != null) setBackground(property.colorPreferences.getSelectedCellBackground());
    }
}
