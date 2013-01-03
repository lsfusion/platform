package platform.fdk.actions.fiscaldatecs;

import java.io.Serializable;

public class UpdateDataOperator implements Serializable {
    public Integer operatorNumber;
    public String operatorName;

    public UpdateDataOperator(Integer operatorNumber, String operatorName) {
        this.operatorNumber = operatorNumber;
        this.operatorName = operatorName;
    }
}
