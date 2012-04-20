package retail.api.remote;

import java.io.Serializable;

public class CashRegisterInfo extends MachineryInfo {
    public String directory;
    public String cashRegisterNumber;

    public CashRegisterInfo(Integer number, String cashRegisterNumber, String nameModel, String handlerModel, String port, String directory) {
        this.cashRegisterNumber = cashRegisterNumber;
        this.number = number;
        this.nameModel = nameModel;
        this.handlerModel = handlerModel;
        this.port = port;
        this.directory = directory;
    }
}
