package lsfusion.server.logics.navigator;

import lsfusion.server.base.AppServerImage;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.interactive.action.async.AsyncExec;

public class NavigatorAction extends NavigatorElement {
    private final Action<?> action;
    private final FormEntity form;

    public NavigatorAction(Action<?> action, String canonicalName, FormEntity form) {
        super(canonicalName);
        
        this.action = action;
        this.form = form;
    }

    @Override
    public String getDefaultIcon() {
        boolean top = isParentRoot();
        if(form != null)
            return top ? AppServerImage.FORMTOP : AppServerImage.FORM;
        return top ? AppServerImage.ACTIONTOP : AppServerImage.ACTION;
    }

    @Override
    public boolean isLeafElement() {
        return true;
    }

    @Override
    public byte getTypeID() {
        return 2;
    }

    @Override
    public AsyncExec getAsyncExec() {
        return Action.getAsyncExec(action.getAsyncEventExec(false));
    }

    public FormEntity getForm() {
        return form;
    }

    public Action getAction() {
        return action;
    }
}