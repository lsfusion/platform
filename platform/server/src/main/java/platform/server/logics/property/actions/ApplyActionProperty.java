package platform.server.logics.property.actions;

import platform.interop.action.ResultClientAction;
import platform.server.classes.ValueClass;
import platform.server.logics.BusinessLogics;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class ApplyActionProperty extends CustomActionProperty {

    BusinessLogics BL;

    public ApplyActionProperty(BusinessLogics BL) {
        super("apply", "Применить изменения", new ValueClass[] {} );

        this.BL = BL;
    }

    @Override
    public void execute(ExecutionContext context) throws SQLException {
        String result = context.applyChanges(BL);
        if (result != null) {
            context.addAction(new ResultClientAction(result, true));
        }
    }
}
