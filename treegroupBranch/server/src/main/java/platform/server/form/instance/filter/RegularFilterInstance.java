package platform.server.form.instance.filter;

import platform.server.form.entity.filter.RegularFilterEntity;

import javax.swing.*;
import java.io.Serializable;

public class RegularFilterInstance implements Serializable {

    private RegularFilterEntity entity;

    public transient FilterInstance filter;

    public int getID() {
        return entity.ID;
    }

    public String getName() {
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
        return getName();
    }
}
