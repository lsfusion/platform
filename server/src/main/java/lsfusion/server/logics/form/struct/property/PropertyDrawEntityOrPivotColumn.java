package lsfusion.server.logics.form.struct.property;

import lsfusion.server.logics.form.interactive.MappingInterface;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawViewOrPivotColumn;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;

public interface PropertyDrawEntityOrPivotColumn<This extends PropertyDrawEntityOrPivotColumn<This>> extends MappingInterface<This> {

    GroupObjectEntity getToDraw(FormEntity form);

    PropertyDrawViewOrPivotColumn getPropertyDrawViewOrPivotColumn(FormView formView);

}
