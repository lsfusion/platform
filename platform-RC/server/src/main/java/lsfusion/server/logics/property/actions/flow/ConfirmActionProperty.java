package lsfusion.server.logics.property.actions.flow;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.interop.action.ConfirmClientAction;
import lsfusion.server.classes.LogicalClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.RequestResult;

import javax.swing.*;
import java.sql.SQLException;

import static lsfusion.base.BaseUtils.toCaption;
import static lsfusion.interop.form.UserInputResult.canceled;

public class ConfirmActionProperty extends MessageActionProperty {
    private final LCP<?> confirmedProperty; // deprecated
    
    private final boolean yesNo;
    private final LCP targetProp;

    public <I extends PropertyInterface> ConfirmActionProperty(LocalizedString caption, String title, LCP confirmedProperty, boolean yesNo, LCP targetProp) {
        super(caption, title);
        this.confirmedProperty = confirmedProperty;
        
        this.yesNo = yesNo;
        this.targetProp = targetProp;
    }

    @Override
    protected void showMessage(ExecutionContext<PropertyInterface> context, Object msgValue) throws SQLException, SQLHandledException {
        Integer result;
        if(msgValue == null) // если NULL считаем что YES
            result = JOptionPane.YES_OPTION;
        else
            result = (Integer) context.requestUserInteraction(
                    new ConfirmClientAction(toCaption(title), String.valueOf(msgValue), yesNo, 0, 0)
            );
        
        assert result != null;
        ImList<RequestResult> requestResults = null;
        if(yesNo) {
            if(result == null || result == JOptionPane.CANCEL_OPTION)
                requestResults = null;
            else
                requestResults = ListFact.singleton(new RequestResult(result == JOptionPane.YES_OPTION ? DataObject.TRUE : NullValue.instance, LogicalClass.instance, targetProp));
        } else {
            if(result != null && result == JOptionPane.YES_OPTION)
                requestResults = ListFact.EMPTY();                
            else
                requestResults = null; // NO_OPTION
        }
        context.writeRequested(requestResults);
        confirmedProperty.change(requestResults == null ? null : true, context);
    }
}
