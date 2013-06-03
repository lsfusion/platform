package lsfusion.server.logics.scripted.proxy;

import lsfusion.server.form.view.GroupObjectView;

public class GroupObjectViewProxy extends ViewProxy<GroupObjectView> {
    public GroupObjectViewProxy(GroupObjectView target) {
        super(target);
    }

    public void setNeedVerticalScroll(Boolean needVerticalScroll) {
        target.needVerticalScroll = needVerticalScroll;
    }

    public void setTableRowsCount(Integer tableRowsCount) {
        target.setTableRowsCount(tableRowsCount);
    }
}
