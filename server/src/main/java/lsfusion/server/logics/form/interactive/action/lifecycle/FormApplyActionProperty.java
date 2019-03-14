package lsfusion.server.logics.form.interactive.action.lifecycle;

import lsfusion.server.language.linear.LP;
import lsfusion.server.logics.classes.ColorClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.property.derived.DerivedProperty;
import lsfusion.server.logics.action.session.DataSession;

import java.awt.*;
import java.sql.SQLException;

public class FormApplyActionProperty extends FormFlowActionProperty {
    private static LP showIf = createShowIfProperty(new Property[]{FormEntity.manageSession}, new boolean[]{false});

    static LP applyBackground = new LP(
            DerivedProperty.createAnd(
                    DerivedProperty.<ClassPropertyInterface>createStatic(Color.green, ColorClass.instance),
                    DataSession.isDataChanged.getImplement()
            ).property
    );

    public FormApplyActionProperty(BaseLogicsModule lm) {
        super(lm);

        drawOptions.addProcessor(new DefaultProcessor() {
            public void proceedDefaultDraw(PropertyDrawEntity entity, FormEntity form) {
                entity.propertyBackground = form.addPropertyObject(applyBackground);
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
        return super.hasFlow(type);
    }
}
