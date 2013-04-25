package platform.server.logics.property.actions.flow;

import com.google.common.base.Throwables;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.interop.action.AsyncGetRemoteChangesClientAction;
import platform.interop.action.UpdateEditValueClientAction;
import platform.server.logics.property.*;
import platform.server.logics.property.derived.DerivedProperty;

import java.io.IOException;
import java.sql.SQLException;

import static platform.base.BaseUtils.serializeObject;

public class AsyncUpdateEditValueAction extends KeepContextActionProperty {
    private final CalcPropertyMapImplement<?, PropertyInterface> valueProperty;

    public <I extends PropertyInterface> AsyncUpdateEditValueAction(String sID, String caption, ImOrderSet<I> innerInterfaces, CalcPropertyMapImplement<?, I> valueProperty) {
        super(sID, caption, innerInterfaces.size());

        if (valueProperty != null) {
            ImRevMap<I, PropertyInterface> mapInterfaces = getMapInterfaces(innerInterfaces).reverse();
            this.valueProperty = valueProperty.map(mapInterfaces);
        } else {
            this.valueProperty = null;
        }
    }

    @Override
    public CalcPropertyMapImplement<?, PropertyInterface> getWhereProperty() {
        return DerivedProperty.createNull();
    }

    @Override
    public ImSet<ActionProperty> getDependActions() {
        return SetFact.EMPTY();
    }

    @Override
    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException {
        if (valueProperty != null) {
            Object updatedValue = valueProperty.read(context, context.getKeys());
            try {
                context.delayUserInteraction(new UpdateEditValueClientAction(serializeObject(updatedValue)));
            } catch (IOException e) {
                Throwables.propagate(e);
            }
        }
        context.delayUserInteraction(new AsyncGetRemoteChangesClientAction());
        return FlowResult.FINISH;
    }
}
