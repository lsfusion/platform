package lsfusion.server.logics.action.flow;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.interop.action.ConfirmClientAction;
import lsfusion.server.logics.classes.LogicalClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.NullValue;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.language.linear.LCP;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.RequestResult;

import javax.swing.*;
import java.sql.SQLException;

import static lsfusion.base.BaseUtils.toCaption;

public class ConfirmActionProperty extends MessageActionProperty {
    
    private final boolean yesNo;
    private final LCP targetProp;

    public <I extends PropertyInterface> ConfirmActionProperty(LocalizedString caption, String title, boolean yesNo, LCP targetProp) {
        super(caption, title);
        
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
    }
}
