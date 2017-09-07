package lsfusion.server.form.view;

import java.awt.*;

public class CalculationsView extends ComponentView {
    public CalculationsView() {}

    public CalculationsView(int ID) {
        super(ID);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(0, -1);
    }
}
