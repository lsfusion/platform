package platform.client.form.queries;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.text.NumberFormat;

public abstract class CalculateSumButton extends ToolbarGridButton {

    public CalculateSumButton() {
        super("/images/sum.png", "Посчитать сумму");
    }

    public abstract void addListener();

    public void showPopupMenu(String caption, Object sum) {
        JPopupMenu menu = new JPopupMenu();
        JLabel label;
        JPanel panel = new JPanel();
        panel.setBackground(new Color(192, 192, 255));
        menu.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED, Color.GRAY, Color.LIGHT_GRAY));

        if (sum != null) {
            label = new JLabel("Сумма [" + caption + "]: ");
            JTextField field = new JTextField(10);
            field.setHorizontalAlignment(JTextField.RIGHT);
            field.setText(format(sum));
            panel.add(label);
            panel.add(field);
        } else {
            label = new JLabel("Невозможно посчитать сумму [" + caption + "]");
            panel.add(label);
        }

        menu.add(panel);
        menu.setLocation(getLocation());
        menu.show(this, menu.getLocation().x + getWidth(), menu.getLocation().y);
    }

    public String format(Object number) {
        NumberFormat nf = NumberFormat.getNumberInstance();
        return nf.format(number);
    }
}
