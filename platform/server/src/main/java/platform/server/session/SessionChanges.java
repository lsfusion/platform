package platform.server.session;

import platform.base.BaseUtils;

public class SessionChanges extends TableChanges<SessionChanges> {

    public SessionChanges() {
    }

    public SessionChanges(SessionChanges changes) {
        super(changes);
    }

    public SessionChanges(SessionChanges changes, ViewDataChanges filter) {
        super(BaseUtils.filterKeys(changes.add,filter.addClasses),BaseUtils.filterKeys(changes.remove,filter.removeClasses),BaseUtils.filterKeys(changes.data,filter.properties));
    }

}
