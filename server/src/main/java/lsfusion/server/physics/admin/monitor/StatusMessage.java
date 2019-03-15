package lsfusion.server.physics.admin.monitor;

public class StatusMessage {
    public final String prefix;
    public final Object property;
    public final int index;
    public final int total;

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