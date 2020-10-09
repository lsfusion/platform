package lsfusion.client.form.property.panel.view;

import lsfusion.client.base.SwingUtils;
import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.design.view.ClientFormLayout;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.EditBindingMap;
import lsfusion.interop.form.event.KeyStrokes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

import static lsfusion.interop.form.event.KeyStrokes.getEnter;

public class DataPanelViewTable extends SingleCellTable {
    private Color backgroundColor;
    private Color foregroundColor;
    private Image image;

    public DataPanelViewTable(ClientFormController form, ClientGroupObjectValue columnKey, ClientPropertyDraw property) {
        super(columnKey, form);

        setProperty(property);

        // хак для ON KEYPRESS, чтобы на ENTER можно было что-нибудь повесить, 
        if (EditBindingMap.getPropertyKeyPressActionSID(getEnter(), property) != null) {
            getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).remove(getEnter());
        }
    }

    public boolean isPressed(int row, int column) {
        return false;
    }

    public Color getBackgroundColor(int row, int column) {
        return backgroundColor;
    }

    public void setBackgroundColor(Color background) {
        this.backgroundColor = background;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public Color getForegroundColor(int row, int column) {
        return foregroundColor;
    }

    public void setForegroundColor(Color foreground) {
        this.foregroundColor = foreground;
    }

    public Color getForegroundColor() {
        return foregroundColor;
    }

    public Image getImage(int row, int column) {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public Image getImage() {
        return image;
    }

    public ClientFormController getForm() {
        return form;
    }

    @Override
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        //hack for keypress 'ENTER' (it should be checked before 'forward-traversal' from SwingUtils.setupSingleCellTable)
        if (form.isDialog() && ks.equals(KeyStrokes.getEnter())) {
            Container parent = getParent();
            while (parent != null) {
                if (parent instanceof ClientFormLayout) {
                    if (((ClientFormLayout) parent).directProcessKeyBinding(ks, e, condition, pressed))
                        return true;
                    break;
                } else {
                    parent = parent.getParent();
                }
            }
        }

        return super.processKeyBinding(ks, e, condition, pressed);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (getProperty().notNull) {
            SwingUtils.paintRightBottomCornerTriangle((Graphics2D) g, 7, SwingDefaults.getNotNullCornerTriangleColor(), 0, 0, getWidth(), getHeight());
        } else if (getProperty().hasChangeAction) {
            SwingUtils.paintRightBottomCornerTriangle((Graphics2D) g, 7, SwingDefaults.getHasChangeActionColor(), 0, 0, getWidth(), getHeight());
        }
    }

    @Override
    public void tabAction(boolean forward) {
    }
}
