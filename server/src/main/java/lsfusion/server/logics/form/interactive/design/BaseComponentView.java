package lsfusion.server.logics.form.interactive.design;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.server.logics.form.struct.FormEntity;

public class BaseComponentView extends ComponentView {

    public BaseComponentView() {
    }

    public BaseComponentView(int ID) {
        super(ID);
    }

    protected FlexAlignment getDefaultAlignment(FormEntity formEntity) {
        ContainerView container = getLayoutParamContainer();
        if (container != null && container.isHorizontal())
            // in horizontal containers base components better looks when they are aligned to the center
            // it's important because form-control / buttons in the bootstrap theme has the height that is different from the height of text / boolean input
            return FlexAlignment.CENTER;

        return super.getDefaultAlignment(formEntity);
    }
}
