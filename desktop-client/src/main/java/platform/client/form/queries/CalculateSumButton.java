package platform.client.form.queries;

import platform.client.ClientResourceBundle;
import platform.client.StartupProperties;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.text.NumberFormat;

public abstract class CalculateSumButton extends ToolbarGridButton {
    private static final ImageIcon sumIcon = new ImageIcon(FilterView.class.getResource("/images/sum.png"));

    public CalculateSumButton() {
        super(sumIcon, ClientResourceBundle.getString("form.queries.calculate.sum"));
    }

    public abstract void addListener();

    public void showPopupMenu(String caption, Object sum) {
        JPopupMenu menu = new JPopupMenu();
        JLabel label;
        JPanel panel = new JPanel();
        panel.setBackground(new Color(192, 192, 255));
        menu.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED, Color.GRAY, Color.LIGHT_GRAY));

        if (sum != null) {
            label = new JLabel(ClientResourceBundle.getString("form.queries.sum.result") + " [" + caption + "]: ");
            JTextField field = new JTextField(10);
            field.setHorizontalAlignment(JTextField.RIGHT);
            field.setText(format(sum));
            panel.add(label);
            panel.add(field);
        } else {
            label = new JLabel(ClientResourceBundle.getString("form.queries.unable.to.calculate.sum") + " [" + caption + "]");
            panel.add(label);
        }

        menu.add(panel);
        menu.setLocation(getLocation());
        menu.show(this, menu.getLocation().x + getWidth(), menu.getLocation().y);
    }

    public String format(Object number) {
        NumberFormat nf = NumberFormat.getNumberInstance();
        if (StartupProperties.dotSeparator)
            return nf.format(number).replace(',', '.');
        else
            return nf.format(number);
    }
}
