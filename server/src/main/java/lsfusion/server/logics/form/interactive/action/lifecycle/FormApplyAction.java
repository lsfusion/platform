package lsfusion.server.logics.form.interactive.action.lifecycle;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.data.ColorClass;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawExtraType;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.awt.*;
import java.sql.SQLException;

public class FormApplyAction extends FormFlowAction {
    private static LP showIf = createShowIfProperty(new Property[]{FormEntity.manageSession}, new boolean[]{false});

    static LP applyBackground = new LP(
            PropertyFact.createAnd(
                    PropertyFact.createStatic(new Color(0, 156, 0), ColorClass.instance),
                    DataSession.isDataChanged.getImplement()
            ).property
    );

    public FormApplyAction(BaseLogicsModule lm) {
        super(lm);

        drawOptions.addProcessor(new DefaultProcessor() {
            public void proceedDefaultDraw(PropertyDrawEntity entity, FormEntity form) {
                entity.setPropertyExtra(form.addPropertyObject(applyBackground), PropertyDrawExtraType.BACKGROUND);
            }
            public void proceedDefaultDesign(PropertyDrawView propertyView) {
            }
        });
    }


    protected void executeForm(FormInstance form, ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        assert context.getEnv() == form;
        context.apply();
    }

    @Override
    protected Property getEnableIf() {
        return DataSession.isDataChanged;
    }

    @Override
    protected LP getShowIf() {
        return showIf;
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        if (type == ChangeFlowType.READONLYCHANGE)  
            return true;
        if (type == ChangeFlowType.HASSESSIONUSAGES)
            return true;
        return super.hasFlow(type);
    }
}
