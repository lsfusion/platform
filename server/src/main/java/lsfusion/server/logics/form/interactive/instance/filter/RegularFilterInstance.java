package lsfusion.server.logics.form.interactive.instance.filter;

import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.logics.form.struct.filter.RegularFilterEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;

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

    public RegularFilterInstance(RegularFilterEntity entity, FilterInstance ifilter) {
        this.entity = entity;
        filter = ifilter;
    }

    public String toString() {
        return ThreadLocalContext.localize(getName());
    }
}
