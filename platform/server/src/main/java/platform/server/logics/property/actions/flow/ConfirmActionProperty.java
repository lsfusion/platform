package platform.server.logics.property.actions.flow;

import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.interop.action.ConfirmClientAction;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.CalcPropertyMapImplement;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.PropertyInterface;

import javax.swing.*;
import java.sql.SQLException;

import static platform.base.BaseUtils.toCaption;

public class ConfirmActionProperty extends MessageActionProperty {
    private final LCP<?> confirmedProperty;

    public <I extends PropertyInterface> ConfirmActionProperty(String sID, String caption, String title, ImOrderSet<I> innerInterfaces, CalcPropertyMapImplement<?, I> msgProperty, LCP confirmedProperty) {
        super(sID, caption, title, innerInterfaces, msgProperty);
        this.confirmedProperty = confirmedProperty;
    }

    @Override
    protected void showMessage(ExecutionContext<PropertyInterface> context, Object msgValue) throws SQLException {
        int result = (Integer)context.requestUserInteraction(
                new ConfirmClientAction(toCaption(title), String.valueOf(msgValue))
        );
        confirmedProperty.change(result == JOptionPane.YES_OPTION ? true : null, context);
    }
}
