package retail.api.remote;

import java.io.Serializable;

public class EquipmentServerSettings implements Serializable {
    public Integer delay;

    public EquipmentServerSettings(Integer delay) {
        this.delay = delay;
    }
}
