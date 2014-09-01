package lsfusion.server.form.entity;

public class ObjectUpdateInfo {
    public UpdateType updateType;
    public boolean onUpdate;
    public Object literal;
    public boolean staticObject;
    
    public ObjectUpdateInfo(UpdateType updateType, boolean onUpdate, Object literal, boolean staticObject) {
        this.updateType = updateType;
        this.onUpdate = onUpdate;
        this.staticObject = staticObject;
        
        if (!staticObject) {
            this.literal = literal;
        } else {
            this.literal = ((String) literal).substring(((String) literal).lastIndexOf(".") + 1);
        }
        
    }

    public ObjectUpdateInfo(UpdateType updateType, Object literal, boolean staticObject) {
        this(updateType, false, literal, staticObject);
    }

    public ObjectUpdateInfo(UpdateType updateType, boolean onUpdate) {
        this(updateType, onUpdate, null, false);
    }
    
    public boolean isFirst() {
        return updateType == UpdateType.FIRST;
    }
    
    public boolean isLast() {
        return updateType == UpdateType.LAST;
    }
    
    public boolean isStatic() {
        return updateType == UpdateType.STATIC;
    }
    
    public enum UpdateType {
        FIRST, LAST, STATIC
    }
}
