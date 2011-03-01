package platform.client.form.queries;

import javax.swing.*;
import java.awt.*;

public abstract class CalculateSumButton extends JButton {
    private Icon icon = new ImageIcon(getClass().getResource("/platform/client/form/images/sum.png"));
    private final Dimension buttonSize = new Dimension(20, 20);

    public CalculateSumButton() {
        super();
        setIcon(icon);
        setAlignmentY(Component.TOP_ALIGNMENT);
        setMinimumSize(buttonSize);
        setPreferredSize(buttonSize);
        setMaximumSize(buttonSize);
        setFocusable(false);
        setToolTipText("Посчитать сумму");
        addListener();
    }

    public abstract void addListener();
}
