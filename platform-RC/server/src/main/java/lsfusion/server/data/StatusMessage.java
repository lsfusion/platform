package lsfusion.server.data;

public class StatusMessage {
    String prefix;
    Object property;
    int index;
    int total;

    public StatusMessage(String prefix, Object property, int index, int total) {
        this.prefix = prefix;
        this.property = property;
        this.index = index;
        this.total = total;
    }

    public String getMessage() {
        return prefix + ": " + index + "/" + total + " " + property.toString();
    }
}