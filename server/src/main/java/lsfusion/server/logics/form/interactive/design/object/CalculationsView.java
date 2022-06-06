package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.server.logics.form.interactive.design.BaseExtraComponentView;
import lsfusion.server.logics.form.struct.FormEntity;

import java.awt.*;

public class CalculationsView extends BaseExtraComponentView {
    public CalculationsView() {}

    public CalculationsView(int ID) {
        super(ID);
    }

    @Override
    protected int getDefaultWidth(FormEntity entity) {
        return 0;
    }
}
