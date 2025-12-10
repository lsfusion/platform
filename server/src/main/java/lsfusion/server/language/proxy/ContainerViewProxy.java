package lsfusion.server.language.proxy;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.server.base.version.Version;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class ContainerViewProxy extends ComponentViewProxy<ContainerView> {

    public ContainerViewProxy(ContainerView target) {
        super(target);
    }

    @SuppressWarnings("unused")
    public void setCaption(Object caption) {
        if(caption instanceof LocalizedString)
            target.setCaption((LocalizedString) caption, getVersion());
        else {
            // we want to keep base name for example for form name prediction in async form opening
            if (target.getCaptionNF(getVersion()) == null) {
                // it's important for web-client to know that caption may appear as container caption is implemented as wrapper panel
                target.setCaption(LocalizedString.NONAME, getVersion());
            }
            target.setPropertyCaption((PropertyObjectEntity<?>) caption, getVersion());
        }
    }

    @SuppressWarnings("unused")
    public void setCaptionClass(Object caption) {
        if(caption instanceof LocalizedString)
            target.setCaptionClass(((LocalizedString) caption).getSourceString(), getVersion());
        else
            target.setPropertyCaptionClass((PropertyObjectEntity<?>) caption, getVersion());
    }

    @SuppressWarnings("unused")
    public void setValueClass(Object caption) {
        if(caption instanceof LocalizedString)
            target.setValueClass(((LocalizedString) caption).getSourceString(), getVersion());
        else
            target.setPropertyValueClass((PropertyObjectEntity<?>) caption, getVersion());
    }

    @SuppressWarnings("unused")
    public void setImage(Object image) {
        if(image instanceof LocalizedString)
            target.setImage(((LocalizedString) image).getSourceString(), null, getVersion()); // should be something else, but for now it's not that important
        else {
            // we want to keep base name for example for form name prediction in async form opening
//            if (target.caption == null) {
                // it's important for web-client to know that caption may appear as container caption is implemented as wrapper panel
//                target.caption = LocalizedString.NONAME;
//            }
            target.setPropertyImage((PropertyObjectEntity<?>) image, getVersion());
        }
    }

    @SuppressWarnings("unused")
    public void setCollapsible(boolean collapsible) {
        target.setCollapsible(collapsible, getVersion());
    }

    @SuppressWarnings("unused")
    public void setPopup(boolean popup) {
        target.setPopup(popup, getVersion());
    }

    @SuppressWarnings("unused")
    public void setBorder(boolean border) {
        target.setBorder(border, getVersion());
    }

    @SuppressWarnings("unused")
    public void setCollapsed(boolean collapsed) {
        target.setCollapsed(collapsed, getVersion());
    }

    @SuppressWarnings("unused")
    public void setHorizontal(boolean horizontal) {
        target.setHorizontal(horizontal, getVersion());
    }

    @SuppressWarnings("unused")
    public void setTabbed(boolean tabbed) {
        target.setTabbed(tabbed, getVersion());
    }

    @SuppressWarnings("unused")
    public void setChildrenAlignment(FlexAlignment childrenAlignment) {
        target.setChildrenAlignment(childrenAlignment, getVersion());
    }

    @SuppressWarnings("unused")
    public void setGrid(boolean grid) {
        target.setGrid(grid, getVersion());
    }

    @SuppressWarnings("unused")
    public void setWrap(boolean wrap) {
        target.setWrap(wrap, getVersion());
    }

    @SuppressWarnings("unused")
    public void setAlignCaptions(boolean alignCaptions) {
        target.setAlignCaptions(alignCaptions, getVersion());
    }

    @SuppressWarnings("unused")
    public void setResizeOverflow(boolean resizeOverflow) {
        target.setResizeOverflow(resizeOverflow, getVersion());
    }

    @SuppressWarnings("unused")
    public void setLines(int lines) {
        target.setLines(lines, getVersion());
    }

    @SuppressWarnings("unused")
    public void setReversed(boolean reversed) {
        target.setReversed(reversed, getVersion());
    }

    @SuppressWarnings("unused")
    public void setLineSize(int lineSize) {
        target.setLineSize(lineSize, getVersion());
    }

    @SuppressWarnings("unused")
    public void setCaptionLineSize(int captionLineSize) {
        target.setCaptionLineSize(captionLineSize, getVersion());
    }

    @SuppressWarnings("unused")
    public void setLineShrink(boolean lineShrink) {
        target.setLineShrink(lineShrink, getVersion());
    }

    @SuppressWarnings("unused")
    public void setCustom(Object caption) {
        if(caption instanceof LocalizedString)
            target.setCustomDesign(caption.toString(), getVersion());
        else
            target.setPropertyCustomDesign((PropertyObjectEntity<?>) caption, getVersion());
    }
    
    // should not be here. added for USERFILTER component backward compatibility
    // as USERFILTER component became FILTERS container in v5.0  
    public void setVisible(boolean visible) {
        if (target.getSID().startsWith("FILTERS") && !visible) {
            target.removeFromParent(Version.current());
        }
    }
}
