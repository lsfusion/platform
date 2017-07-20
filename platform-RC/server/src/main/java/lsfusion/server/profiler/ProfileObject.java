package lsfusion.server.profiler;

import lsfusion.base.BaseUtils;

import static lsfusion.base.BaseUtils.nullEquals;
import static lsfusion.base.BaseUtils.nullHash;

public abstract class ProfileObject {
    protected Object[] objects;
    
    public ProfileObject(Object... objects) {
        this.objects = objects;
    }

    @Override
    public int hashCode() {
        if (objects.length <= 0) {
            return super.hashCode();
        }
        int hash = 0;
        for (Object object : objects) {
            hash = (hash + nullHash(object)) * 31;
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof ProfileObject && objects.length == ((ProfileObject) obj).objects.length) {
            for (int i = 0; i < objects.length; i++) {
                if (!nullEquals(objects[i], ((ProfileObject) obj).objects[i])) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public String getProfileString() {
        return toString();
    }

    @Override
    public String toString() {
        String s = "";
        if (objects.length > 0) {
            s += BaseUtils.toString(", ", objects);
        }
        return s;
    }
}
