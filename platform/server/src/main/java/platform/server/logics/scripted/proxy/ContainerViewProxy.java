package platform.server.logics.scripted.proxy;

import platform.server.form.view.ContainerView;

public class ContainerViewProxy extends ComponentViewProxy<ContainerView> {

    public ContainerViewProxy(ContainerView target) {
        super(target);
    }
    
    public void setCaption(String caption) {
        setTitle(caption);
    }

    public void setTitle(String title) {
        target.title = title;
    }

    public void setDescription(String description) {
        target.description = description;
    }

    public void setTabbedPane(boolean tabbedPane) {
        target.tabbedPane = tabbedPane;
    }
}
