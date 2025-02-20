package lsfusion.server.logics.action.interactive;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.action.ConfirmClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.data.LogicalClass;
import lsfusion.server.logics.form.interactive.action.input.RequestResult;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import javax.swing.*;
import java.sql.SQLException;

import static lsfusion.base.BaseUtils.toCaption;

public class ConfirmAction extends MessageAction {
    
    private final boolean yesNo;
    private final LP targetProp;

    public ConfirmAction(LocalizedString caption, boolean hasHeader, boolean yesNo, LP targetProp) {
        super(caption, hasHeader);
        
        this.yesNo = yesNo;
        this.targetProp = targetProp;
        assert !(yesNo && targetProp == null);
    }

    @Override
    protected void showMessage(ExecutionContext<PropertyInterface> context, String message, String header) throws SQLException, SQLHandledException {
        Integer result;
        if(message == null) // если NULL считаем что YES
            result = JOptionPane.YES_OPTION;
        else
            result = (Integer) context.requestUserInteraction(
                    new ConfirmClientAction(toCaption(header), message, yesNo, 0, 0)
            );
        
        assert result != null;
        ImList<RequestResult> requestResults;
        if(yesNo) {
            if(result == JOptionPane.CANCEL_OPTION)
                requestResults = null;
            else
                requestResults = ListFact.singleton(new RequestResult(result == JOptionPane.YES_OPTION ? DataObject.TRUE : NullValue.instance, LogicalClass.instance, targetProp));
        } else {
            if(result == JOptionPane.YES_OPTION)
                requestResults = ListFact.EMPTY();                
            else
                requestResults = null; // NO_OPTION
        }
        context.writeRequested(requestResults);
    }

    @Override
    protected ImMap<Property, Boolean> aspectChangeExtProps(ImSet<Action<?>> recursiveAbstracts) {
        return getRequestChangeExtProps(yesNo ? 1 : 0, index -> LogicalClass.instance, index -> targetProp);
    }
}
