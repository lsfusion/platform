package lsfusion.server.language.proxy;

import lsfusion.server.logics.form.interactive.design.object.ClassChooserView;

public class ClassChooserViewProxy extends ComponentViewProxy<ClassChooserView> {
    public ClassChooserViewProxy(ClassChooserView target) {
        super(target);
    }

    public void setVisible(boolean visible) {
        target.visible = visible;
    }
}
