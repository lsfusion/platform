package platform.client.form.queries;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;

public abstract class CountQuantityButton extends BaseGridButton {

    public CountQuantityButton() {
        super("/images/quantity.png", "Количество записей");
    }

    public abstract void addListener();

    public void showPopupMenu(int quantity) {
        JPopupMenu menu = new JPopupMenu();
        JLabel label = new JLabel("Количество записей: " + quantity);
        JPanel panel = new JPanel();
        panel.setBackground(new Color(192, 192, 255));
        menu.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED, Color.GRAY, Color.LIGHT_GRAY));
        panel.add(label);
        menu.add(panel);
        menu.setLocation(getLocation());
        menu.show(this, menu.getLocation().x + getWidth(), menu.getLocation().y);
    }
}
