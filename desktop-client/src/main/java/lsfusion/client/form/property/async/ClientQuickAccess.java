package lsfusion.client.form.property.async;

public class ClientQuickAccess {
    public ClientQuickAccessMode mode;
    public Boolean hover;

    public ClientQuickAccess(ClientQuickAccessMode mode, Boolean hover) {
        this.mode = mode;
        this.hover = hover;
    }
}