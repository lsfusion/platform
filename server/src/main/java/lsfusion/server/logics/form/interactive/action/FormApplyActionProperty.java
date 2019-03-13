package lsfusion.server.logics.form.interactive.action;

import lsfusion.server.classes.ColorClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.language.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.property.derived.DerivedProperty;
import lsfusion.server.session.DataSession;

import java.awt.*;
import java.sql.SQLException;

public class FormApplyActionProperty extends FormFlowActionProperty {
    private static LCP showIf = createShowIfProperty(new CalcProperty[]{FormEntity.manageSession}, new boolean[]{false});

    static LCP applyBackground = new LCP(
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
    protected CalcProperty getEnableIf() {
        return DataSession.isDataChanged;
    }

    @Override
    protected LCP getShowIf() {
        return showIf;
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        if (type == ChangeFlowType.READONLYCHANGE)  
            return true;
        return super.hasFlow(type);
    }
}
