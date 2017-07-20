package lsfusion.server.logics.property;

public enum PropertyQueryType {
    NOCHANGE, CHANGED, FULLCHANGED, RECURSIVE;
    
    public boolean needChange() {
        return this != NOCHANGE;
    }
}
