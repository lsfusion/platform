package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.struct.FormEntity;

import java.awt.*;

public class CalculationsView extends ComponentView {
    public CalculationsView() {}

    public CalculationsView(int ID) {
        super(ID);
    }

    @Override
    public Dimension getSize(FormEntity entity) {
        return new Dimension(0, -1);
    }
}
