package lsfusion.server.language.proxy;

import lsfusion.server.logics.form.interactive.design.object.TreeGroupView;

public class TreeGroupViewProxy extends ComponentViewProxy<TreeGroupView> {
    public TreeGroupViewProxy(TreeGroupView target) {
        super(target);
    }
    
    public void setExpandOnClick(boolean expandOnClick) {
        target.expandOnClick = expandOnClick;
    }

    public void setHeaderHeight(int headerHeight) {
        target.headerHeight = headerHeight;
    }
}
