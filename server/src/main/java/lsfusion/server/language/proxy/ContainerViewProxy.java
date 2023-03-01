package lsfusion.server.language.proxy;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.design.ContainerType;
import lsfusion.server.base.version.Version;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class ContainerViewProxy extends ComponentViewProxy<ContainerView> {

    public ContainerViewProxy(ContainerView target) {
        super(target);
    }
    
    public void setCaption(Object caption) {
        if(caption instanceof LocalizedString)
            target.setCaption((LocalizedString) caption);
        else {
            // we want to keep base name for example for form name prediction in async form opening
            if (target.caption == null) {
                // it's important for web-client to know that caption may appear as container caption is implemented as wrapper panel
                target.caption = LocalizedString.NONAME;
            }
            target.propertyCaption = (PropertyObjectEntity<?>) caption;
        }
    }

    public void setImage(Object image) {
        if(image instanceof LocalizedString)
            target.setImage(((LocalizedString) image).getSourceString(), null); // should be something else, but for now it's not that important
        else {
            // we want to keep base name for example for form name prediction in async form opening
//            if (target.caption == null) {
                // it's important for web-client to know that caption may appear as container caption is implemented as wrapper panel
//                target.caption = LocalizedString.NONAME;
//            }
            target.propertyImage = (PropertyObjectEntity<?>) image;
        }
    }
    
    public void setCollapsible(boolean collapsible) {
        target.setCollapsible(collapsible);
    }

    public void setBorder(boolean border) {
        target.setBorder(border);
    }

    public void setCollapsed(boolean collapsed) {
        target.setCollapsed(collapsed);
    }
    
    public void setType(ContainerType type) {
        target.setType(type);
    }

    public void setHorizontal(boolean horizontal) {
        target.setHorizontal(horizontal);
    }

    public void setTabbed(boolean tabbed) {
        target.setTabbed(tabbed);
    }
    
    public void setChildrenAlignment(FlexAlignment falign) {
        target.setChildrenAlignment(falign);
    }
    
    public void setAlignCaptions(boolean alignCaptions) {
        target.setAlignCaptions(alignCaptions);
    }

    public void setGrid(boolean grid) {
        target.setGrid(grid);
    }

    public void setWrap(boolean wrap) {
        target.setWrap(wrap);
    }

    public void setResizeOverflow(boolean resizeOverflow) {
        target.resizeOverflow = resizeOverflow;
    }

    public void setCustom(Object caption) {
        if(caption instanceof LocalizedString)
            target.setCustomDesign(caption.toString());
        else
            target.setPropertyCustomDesign((PropertyObjectEntity<?>) caption);
    }

    //backward compatibility
    public void setColumns(int columns) {
        target.lines = columns;
    }

    public void setLines(int lines) {
        target.lines = lines;
    }

    public void setLineSize(int lineSize) {
        target.lineSize = lineSize;
    }

    public void setCaptionLineSize(int lineSize) {
        target.captionLineSize = lineSize;
    }
    
    
    // should not be here. added for USERFILTER component backward compatibility
    // as USERFILTER component became FILTERS container in v5.0  
    public void setVisible(boolean visible) {
        if (target.getSID().startsWith("FILTERS") && !visible) {
            target.removeFromParent(Version.current());
        }
    }
}
