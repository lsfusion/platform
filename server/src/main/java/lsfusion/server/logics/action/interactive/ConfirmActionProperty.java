package lsfusion.server.logics.action.interactive;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.interop.action.ConfirmClientAction;
import lsfusion.server.data.DataObject;
import lsfusion.server.data.NullValue;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.language.linear.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.LogicalClass;
import lsfusion.server.logics.form.interactive.action.input.RequestResult;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import javax.swing.*;
import java.sql.SQLException;

import static lsfusion.base.BaseUtils.toCaption;

public class ConfirmActionProperty extends MessageAction {
    
    private final boolean yesNo;
    private final LP targetProp;

    public <I extends PropertyInterface> ConfirmActionProperty(LocalizedString caption, String title, boolean yesNo, LP targetProp) {
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
