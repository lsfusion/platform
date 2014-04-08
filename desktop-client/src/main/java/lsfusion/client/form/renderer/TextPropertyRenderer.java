package lsfusion.client.form.renderer;

import lsfusion.base.BaseUtils;
import lsfusion.client.form.PropertyRenderer;
import lsfusion.client.form.editor.rich.RichEditorKit;
import lsfusion.client.form.editor.rich.RichEditorPane;
import lsfusion.client.logics.ClientPropertyDraw;

import javax.swing.*;
import java.awt.*;


public class TextPropertyRenderer extends JEditorPane implements PropertyRenderer {

    private final boolean rich;

    private Color defaultBackground;

    public TextPropertyRenderer(ClientPropertyDraw property, boolean rich) {
        this.rich = rich;

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
        if (isSelected) {
            if (hasFocus) {
                setBorder(BorderFactory.createCompoundBorder(FOCUSED_CELL_BORDER, BorderFactory.createEmptyBorder(1, 2, 0, 1)));
                setBackground(FOCUSED_CELL_BACKGROUND);
            } else {
                setBorder(BorderFactory.createCompoundBorder(SELECTED_ROW_BORDER, BorderFactory.createEmptyBorder(1, 3, 0, 2)));
                setBackground(SELECTED_ROW_BACKGROUND);
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
        if (BaseUtils.isRedundantString(value)) {
            setContentType("text");
            setText(EMPTY_STRING);
            setForeground(UIManager.getColor("TextField.inactiveForeground"));
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
        setBackground(PropertyRenderer.SELECTED_CELL_BACKGROUND);
    }
}
