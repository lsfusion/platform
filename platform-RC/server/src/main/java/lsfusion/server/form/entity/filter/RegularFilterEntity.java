package lsfusion.server.form.entity.filter;

import lsfusion.base.identity.IdentityObject;
import lsfusion.server.logics.i18n.LocalizedString;

import javax.swing.*;

public class RegularFilterEntity extends IdentityObject {
    public transient FilterEntity filter;
    public LocalizedString name;
    public KeyStroke key;
    public boolean showKey = true;

    public RegularFilterEntity() {

    }

    public RegularFilterEntity(int iID, FilterEntity ifilter, LocalizedString iname) {
        this(iID, ifilter, iname, null);
    }

    public RegularFilterEntity(int iID, FilterEntity ifilter, LocalizedString iname, KeyStroke ikey) {
        ID = iID;
        filter = ifilter;
        name = iname;
        key = ikey;
    }
}
