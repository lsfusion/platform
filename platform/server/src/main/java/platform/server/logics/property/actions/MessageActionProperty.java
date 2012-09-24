package platform.server.logics.property.actions;

import platform.base.BaseUtils;
import platform.interop.action.MessageClientAction;
import platform.server.classes.StringClass;
import platform.server.classes.ValueClass;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class MessageActionProperty extends SystemActionProperty {
    private ClassPropertyInterface msgInterface;

    public MessageActionProperty(String sID, String caption, int length) {
        super(sID, caption, new ValueClass[]{StringClass.get(length)});
        msgInterface = BaseUtils.single(interfaces);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.requestUserInteraction(
                new MessageClientAction(((String) context.getKeyValue(msgInterface).object).trim(), caption)
        );
    }
}
