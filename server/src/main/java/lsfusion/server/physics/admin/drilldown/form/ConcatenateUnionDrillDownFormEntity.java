package lsfusion.server.physics.admin.drilldown.form;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.property.classes.data.FormulaUnionProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class ConcatenateUnionDrillDownFormEntity<I extends PropertyInterface> extends UnionDrillDownFormEntity<FormulaUnionProperty.Interface, FormulaUnionProperty> {

    public ConcatenateUnionDrillDownFormEntity(LocalizedString caption, FormulaUnionProperty property, LogicsModule LM) {
        super(caption, property, LM);
    }
}