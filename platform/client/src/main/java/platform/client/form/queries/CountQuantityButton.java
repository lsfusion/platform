package platform.client.form.queries;

import javax.swing.*;
import java.awt.*;

public abstract class CountQuantityButton extends JButton {
    private Icon icon = new ImageIcon(getClass().getResource("/platform/client/form/images/quantity.png"));
    private final Dimension buttonSize = new Dimension(20, 20);

    public CountQuantityButton() {
        super();
        setIcon(icon);
        setAlignmentY(Component.TOP_ALIGNMENT);
        setMinimumSize(buttonSize);
        setPreferredSize(buttonSize);
        setMaximumSize(buttonSize);
        setFocusable(false);
        addListener();
    }

    public abstract void addListener();
}
