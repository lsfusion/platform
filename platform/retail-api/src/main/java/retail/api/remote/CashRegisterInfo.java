package retail.api.remote;

import java.io.Serializable;

public class CashRegisterInfo extends MachineryInfo {
    public String directory;

    public CashRegisterInfo(Integer number, String nameModel, String handlerModel, String port, String directory) {
        this.number = number;
        this.nameModel = nameModel;
        this.handlerModel = handlerModel;
        this.port = port;
        this.directory = directory;
    }
}
