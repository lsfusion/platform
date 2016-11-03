package lsfusion.server.logics.property.actions.form;

import lsfusion.server.classes.ColorClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.view.PropertyDrawView;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.derived.DerivedProperty;
import lsfusion.server.session.DataSession;

import java.awt.*;
import java.sql.SQLException;

public class FormApplyActionProperty extends FormFlowActionProperty {
    private static LCP showIf = createShowIfProperty(new CalcProperty[]{FormEntity.manageSession, FormEntity.isReadOnly}, new boolean[]{false, true});

    static LCP applyBackground = new LCP(
            DerivedProperty.createAnd(
                    DerivedProperty.<ClassPropertyInterface>createStatic(Color.green, ColorClass.instance),
                    DataSession.isDataChanged.getImplement()
            ).property
    );

    public FormApplyActionProperty(BaseLogicsModule lm) {
        super(lm);

        drawOptions.addProcessor(new DefaultProcessor() {
            public void proceedDefaultDraw(PropertyDrawEntity entity, FormEntity<?> form) {
                entity.propertyBackground = form.addPropertyObject(applyBackground);
            }
            public void proceedDefaultDesign(PropertyDrawView propertyView) {
            }
        });
    }


    protected void executeForm(FormInstance form, ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        form.formApply(context);
    }

    @Override
    protected CalcProperty getEnableIf() {
        return DataSession.isDataChanged;
    }

    @Override
    protected LCP getShowIf() {
        return showIf;
    }
}
