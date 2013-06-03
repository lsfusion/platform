package lsfusion.server.logics.scripted.proxy;

import lsfusion.server.form.view.ContainerView;

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

    public void setType(byte type) {
        target.setType(type);
    }
}
