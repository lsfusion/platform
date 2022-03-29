package lsfusion.server.logics.form.interactive.action.expand;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

public class ExpandCollapseContainerAction extends SystemExplicitAction {
    private ComponentView component;
    private boolean collapse;

    public ExpandCollapseContainerAction(LocalizedString caption, ComponentView component, boolean collapse) {
        super(caption);
        this.component = component;
        this.collapse = collapse;
    }
    
    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        if (component instanceof ContainerView && ((ContainerView) component).isCollapsible()) {
            FormInstance formInstance = context.getFormInstance(false, true);
            if (collapse) {
                formInstance.collapseContainer((ContainerView) component);    
            } else {
                formInstance.expandContainer((ContainerView) component);
            }
        }
    }
}
