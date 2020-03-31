package lsfusion.client.form.object.table.grid.user.toolbar.view;

import lsfusion.client.ClientResourceBundle;

import javax.swing.*;

public abstract class CountQuantityButton extends ToolbarGridButton {
    private static final String QUANTITY_ICON_PATH = "quantity.png";

    public CountQuantityButton() {
        super(QUANTITY_ICON_PATH, ClientResourceBundle.getString("form.queries.number.of.entries"));
    }

    public abstract void addListener();

    public void showPopupMenu(Integer quantity) {
        JPopupMenu menu = new JPopupMenu();
        JLabel label = new JLabel(ClientResourceBundle.getString("form.queries.number.of.entries") + ": " + (quantity == null ? 0 : quantity));
        JPanel panel = new JPanel();
        panel.add(label);
        menu.add(panel);
        menu.setLocation(getLocation());
        menu.show(this, menu.getLocation().x + getWidth(), menu.getLocation().y);
    }
}
