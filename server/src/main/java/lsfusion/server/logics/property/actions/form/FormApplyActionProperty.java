package lsfusion.server.logics.property.actions.form;

import lsfusion.server.classes.ColorClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.derived.DerivedProperty;
import lsfusion.server.session.DataSession;

import java.awt.*;
import java.sql.SQLException;

public class FormApplyActionProperty extends FormToolbarActionProperty {
    private static LCP showIf = createShowIfProperty(new CalcProperty[]{FormEntity.manageSession, FormEntity.isReadOnly}, new boolean[]{false, true});

    static LCP applyBackground = new LCP(
            DerivedProperty.createAnd(
                    DerivedProperty.<ClassPropertyInterface>createStatic(Color.green, ColorClass.instance),
                    DataSession.isDataChanged.getImplement()
            ).property
    );

    public FormApplyActionProperty(BaseLogicsModule lm) {
        super(lm);
    }


    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        context.getFormInstance().formApply(context);
    }

    @Override
    protected CalcProperty getEnableIf() {
        return DataSession.isDataChanged;
    }

    @Override
    public void proceedDefaultDraw(PropertyDrawEntity<ClassPropertyInterface> propertyDraw, FormEntity<?> form, Version version) {
        super.proceedDefaultDraw(propertyDraw, form, version);

        propertyDraw.propertyBackground = form.addPropertyObject(applyBackground);
    }

    @Override
    protected LCP getShowIf() {
        return showIf;
    }
}
