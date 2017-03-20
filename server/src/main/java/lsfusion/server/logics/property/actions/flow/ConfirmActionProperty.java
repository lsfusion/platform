package lsfusion.server.logics.property.actions.flow;

import lsfusion.base.col.ListFact;
import lsfusion.interop.action.ConfirmClientAction;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.RequestResult;

import javax.swing.*;
import java.sql.SQLException;

import static lsfusion.base.BaseUtils.toCaption;

public class ConfirmActionProperty extends MessageActionProperty {
    private final LCP<?> confirmedProperty;

    public <I extends PropertyInterface> ConfirmActionProperty(LocalizedString caption, String title, LCP confirmedProperty) {
        super(caption, title);
        this.confirmedProperty = confirmedProperty;
    }

    @Override
    protected void showMessage(ExecutionContext<PropertyInterface> context, Object msgValue) throws SQLException, SQLHandledException {
        Integer result = (Integer)context.requestUserInteraction(
                new ConfirmClientAction(toCaption(title), String.valueOf(msgValue))
        );
        Boolean confirmed = result != null && result == JOptionPane.YES_OPTION ? true : null;
        confirmedProperty.change(confirmed, context);
        context.writeRequested(confirmed != null ? ListFact.<RequestResult>EMPTY() : null);
    }
}
