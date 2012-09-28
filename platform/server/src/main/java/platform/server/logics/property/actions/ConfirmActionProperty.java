package platform.server.logics.property.actions;

import platform.base.BaseUtils;
import platform.interop.action.ConfirmClientAction;
import platform.server.classes.StringClass;
import platform.server.classes.ValueClass;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import javax.swing.*;
import java.sql.SQLException;

import static platform.base.BaseUtils.toCaption;

public class ConfirmActionProperty extends SystemActionProperty {
    private final LCP<?> confirmedProperty;

    private final ClassPropertyInterface msgInterface;

    public ConfirmActionProperty(String sID, String caption, int length, LCP confirmedProperty) {
        super(sID, caption, new ValueClass[]{StringClass.get(length)});

        this.confirmedProperty = confirmedProperty;
        this.msgInterface = BaseUtils.single(interfaces);
    }

    @Override
    public PropsNewSession aspectChangeExtProps() {
        return getChangeProps(confirmedProperty.property);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        String message = (String) context.getKeyValue(msgInterface).object;

        int result = (Integer)context.requestUserInteraction(
                new ConfirmClientAction(toCaption(caption), toCaption(message))
        );

        confirmedProperty.change(result == JOptionPane.YES_OPTION ? true : null, context);
    }
}
