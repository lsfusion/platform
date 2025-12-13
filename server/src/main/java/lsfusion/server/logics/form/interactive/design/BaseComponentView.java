package lsfusion.server.logics.form.interactive.design;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.ServerIdentityObject;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.FormInstanceContext;

public abstract class BaseComponentView<This extends BaseComponentView<This, AddParent>, AddParent extends ServerIdentityObject<AddParent, ?>> extends ComponentView<This, AddParent> {

    public BaseComponentView() {
    }

    protected FlexAlignment getDefaultAlignment(FormInstanceContext context) {
        ContainerView container = getLayoutParamContainer();
        if (container != null && container.isHorizontal())
            // in horizontal containers base components better looks when they are aligned to the center
            // it's important because form-control / buttons in the bootstrap theme has the height that is different from the height of text / boolean input
            return FlexAlignment.CENTER;

        return super.getDefaultAlignment(context);
    }

    // copy-constructor
    protected BaseComponentView(This src, ObjectMapping mapping) {
        super(src, mapping);
    }
}
