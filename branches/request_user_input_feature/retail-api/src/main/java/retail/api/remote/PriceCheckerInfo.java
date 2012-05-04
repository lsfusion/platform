package retail.api.remote;

import java.io.Serializable;

public class PriceCheckerInfo extends MachineryInfo {

    public PriceCheckerInfo(Integer number, String nameModel, String handlerModel, String port) {
        this.number = number;
        this.nameModel = nameModel;
        this.handlerModel = handlerModel;
        this.port = port;
    }
}
