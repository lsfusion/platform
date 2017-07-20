package lsfusion.server.logics.property;

public enum IncrementType {
    SUSPICION, DROPSET, SET, DROP, CHANGED, SETCHANGED, DROPCHANGED;
    
    public boolean isNotNullNew() {
        return this == SET || this == SETCHANGED;
    }

    public Boolean getSetOrDropped() {
        if(this == SET)
            return true;
        if(this == DROP)
            return false;
        return null;
    }
}
