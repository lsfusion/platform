package lsfusion.server.form.navigator;

import lsfusion.server.physics.dev.i18n.LocalizedString;

public class NavigatorFolder extends NavigatorElement {
    public NavigatorFolder(String canonicalName, LocalizedString caption) {
        super(canonicalName, caption);

        setImage("/images/open.png", DefaultIcon.OPEN);
    }

    @Override
    public boolean isLeafElement() {
        return false;
    }

    @Override
    public byte getTypeID() {
        return 1;
    }
}
