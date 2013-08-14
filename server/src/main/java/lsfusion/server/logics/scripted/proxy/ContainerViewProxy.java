package lsfusion.server.logics.scripted.proxy;

import lsfusion.interop.form.layout.*;
import lsfusion.server.form.view.ContainerView;

public class ContainerViewProxy extends ComponentViewProxy<ContainerView> {

    public ContainerViewProxy(ContainerView target) {
        super(target);
    }
    
    public void setCaption(String caption) {
        target.caption = caption;
    }

    public void setDescription(String description) {
        target.description = description;
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

    public void setGapX(int gapX) {
        target.gapX = gapX;
    }

    public void setGapY(int gapY) {
        target.gapY = gapY;
    }

    //todo: remove
    public void setChildConstraints(DoNotIntersectSimplexConstraint childConstraints) {
        if (childConstraints == SingleSimplexConstraint.TOTHE_BOTTOM) {
            setType(ContainerType.CONTAINERV);
        } else if (childConstraints == SingleSimplexConstraint.TOTHE_RIGHT) {
            setType(ContainerType.CONTAINERH);
        } else if (childConstraints == SingleSimplexConstraint.TOTHE_RIGHTBOTTOM) {
            setType(ContainerType.CONTAINERH);
        }
    }
}
