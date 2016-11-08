package lsfusion.server.form.instance.filter;

import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.form.entity.filter.RegularFilterEntity;
import lsfusion.server.logics.i18n.LocalizedString;

import javax.swing.*;
import java.io.Serializable;

public class RegularFilterInstance implements Serializable {

    private RegularFilterEntity entity;

    public transient FilterInstance filter;

    public int getID() {
        return entity.ID;
    }

    public LocalizedString getName() {
        return entity.name;
    }

    public KeyStroke getKey() {
        return entity.key;
    }

    public boolean isShowKey() {
        return entity.showKey;
    }

    public RegularFilterInstance(RegularFilterEntity entity, FilterInstance ifilter) {
        this.entity = entity;
        filter = ifilter;
    }

    public String toString() {
        return ThreadLocalContext.localize(getName());
    }
}
