package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.server.logics.form.interactive.design.BaseComponentView;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.struct.FormEntity;

import java.awt.*;

public class CalculationsView extends BaseComponentView {
    public CalculationsView() {}

    public CalculationsView(int ID) {
        super(ID);
    }

    @Override
    protected int getDefaultWidth(FormEntity entity) {
        return 0;
    }
}
