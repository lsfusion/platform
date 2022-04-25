package lsfusion.server.logics.form.struct;

import lsfusion.server.logics.form.struct.action.ActionObjectEntity;

import java.io.DataOutputStream;
import java.io.IOException;

public class FormScheduler {
    public ActionObjectEntity actionObjectEntity;
    int period;
    boolean fixed;

    public FormScheduler(ActionObjectEntity actionObjectEntity, int period, boolean fixed) {
        this.actionObjectEntity = actionObjectEntity;
        this.period = period;
        this.fixed = fixed;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeInt(period);
        outStream.writeBoolean(fixed);
    }
}