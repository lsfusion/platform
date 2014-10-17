package lsfusion.server.logics.scripted.proxy;

import lsfusion.server.form.view.TreeGroupView;

public class TreeGroupViewProxy extends ComponentViewProxy<TreeGroupView> {
    public TreeGroupViewProxy(TreeGroupView target) {
        super(target);
    }
    
    public void setExpandOnClick(boolean expandOnClick) {
        target.expandOnClick = expandOnClick;
    }
}
