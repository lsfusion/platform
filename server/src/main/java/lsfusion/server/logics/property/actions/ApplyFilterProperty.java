package lsfusion.server.logics.property.actions;

import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.session.ApplyFilter;

import java.sql.SQLException;

public class ApplyFilterProperty extends SystemExplicitActionProperty {

    private final ApplyFilter type;

    public ApplyFilterProperty(ApplyFilter type) {
        super(type.getSID(), type.getSID());
        this.type = type;
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.getSession().setApplyFilter(type);
    }
}
