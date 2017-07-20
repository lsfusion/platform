package lsfusion.interop.event;

public class ValueEvent {
    private String SID;
    private Object value;

    public ValueEvent(String SID, Object value) {
        this.SID = SID;
        this.value = value;
    }

    public String getSID() {
        return SID;
    }

    public Object getValue(){
        return value;
    }

}
