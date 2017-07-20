package lsfusion.server.logics.scripted.proxy;

import lsfusion.interop.form.layout.Alignment;
import lsfusion.interop.form.layout.ContainerType;
import lsfusion.interop.form.layout.FlexAlignment;
import lsfusion.server.form.entity.CalcPropertyObjectEntity;
import lsfusion.server.form.view.ContainerView;
import lsfusion.server.logics.i18n.LocalizedString;

public class ContainerViewProxy extends ComponentViewProxy<ContainerView> {

    public ContainerViewProxy(ContainerView target) {
        super(target);
    }
    
    public void setCaption(String caption) {
        target.caption = LocalizedString.create(caption);
    }

    public void setDescription(String description) {
        target.description = LocalizedString.create(description);
    }

    public void setColumnLabelsWidth(int columnLabelsWidth) {
        target.columnLabelsWidth = columnLabelsWidth;
    }
    
    public void setType(ContainerType type) {
        target.setType(type);
    }
    
    public void setChildrenAlignment(FlexAlignment falign) {
        Alignment align;
        switch (falign) {
            case LEADING: align = Alignment.LEADING; break;
            case CENTER: align = Alignment.CENTER; break;
            case TRAILING: align = Alignment.TRAILING; break;
            default:
                throw new IllegalStateException("Children alignment should be either of LEADING, CENTER, TRAILING");
        }
        target.setChildrenAlignment(align);
    }

    public void setColumns(int columns) {
        target.columns = columns;
    }

    public void setShowIf(CalcPropertyObjectEntity<?> showIf) {
        target.setShowIf(showIf);
    }
}
