package platform.server.logics.scripted.proxy;

import platform.server.form.view.FunctionView;

public class FunctionViewProxy extends ComponentViewProxy<FunctionView> {

    public FunctionViewProxy(FunctionView target) {
        super(target);
    }

    public void setCaption(String caption) {
        target.setCaption(caption);
    }

    public void setVisible(boolean visible) {
        target.setVisible(visible);
    }
}
