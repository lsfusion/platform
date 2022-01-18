package lsfusion.server.physics.admin.drilldown.form;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.property.classes.data.ConcatenateUnionProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class ConcatenateUnionDrillDownFormEntity<I extends PropertyInterface> extends UnionDrillDownFormEntity<ConcatenateUnionProperty.Interface, ConcatenateUnionProperty> {

    public ConcatenateUnionDrillDownFormEntity(LocalizedString caption, ConcatenateUnionProperty property, LogicsModule LM) {
        super(caption, property, LM);
    }
}