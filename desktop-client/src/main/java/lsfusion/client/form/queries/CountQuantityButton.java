package lsfusion.client.form.queries;

import lsfusion.client.ClientResourceBundle;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;

public abstract class CountQuantityButton extends ToolbarGridButton {
    private static final ImageIcon quantityIcon = new ImageIcon(FilterView.class.getResource("/images/quantity.png"));

    public CountQuantityButton() {
        super(quantityIcon, ClientResourceBundle.getString("form.queries.number.of.entries"));
    }

    public abstract void addListener();

    public void showPopupMenu(Integer quantity) {
        JPopupMenu menu = new JPopupMenu();
        JLabel label = new JLabel(ClientResourceBundle.getString("form.queries.number.of.entries") + ": " + (quantity == null ? 0 : quantity));
        JPanel panel = new JPanel();
        panel.setBackground(new Color(192, 192, 255));
        menu.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED, Color.GRAY, Color.LIGHT_GRAY));
        panel.add(label);
        menu.add(panel);
        menu.setLocation(getLocation());
        menu.show(this, menu.getLocation().x + getWidth(), menu.getLocation().y);
    }
}
