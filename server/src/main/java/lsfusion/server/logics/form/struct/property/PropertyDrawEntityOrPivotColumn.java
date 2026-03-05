package lsfusion.server.logics.form.struct.property;

import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawViewOrPivotColumn;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;

public interface PropertyDrawEntityOrPivotColumn {

    GroupObjectEntity getToDraw(FormEntity form);

    PropertyDrawViewOrPivotColumn getPropertyDrawViewOrPivotColumn(FormView formView);

}
