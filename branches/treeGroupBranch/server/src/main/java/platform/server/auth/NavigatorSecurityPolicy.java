package platform.server.auth;

import platform.server.form.navigator.NavigatorElement;

import java.util.Collection;

public class NavigatorSecurityPolicy extends AbstractSecurityPolicy<NavigatorElement> {
    @Override
    public void permit(NavigatorElement element) {
        if (element != null) {
            super.permit(element);
            permit(element.getParent());
        }
    }

    @Override
    public void deny(NavigatorElement element) {
        if (element != null) {
            super.deny(element);
            super.deny(element.getChildren(true));
        }
    }

    @Override
    public void permit(Collection<? extends NavigatorElement> colObj) {
        for (NavigatorElement e : colObj) {
            permit(e);
        }
    }

    @Override
    public void deny(Collection<? extends NavigatorElement> colObj) {
        for (NavigatorElement e : colObj) {
            deny(e);
        }
    }
}
