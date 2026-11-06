package lsfusion.server.logics.form.stat.print.design;

import lsfusion.interop.form.print.ReportFieldExtraType;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// поле для отрисовки отчета
public class ReportDrawField implements AbstractRowLayoutElement {

    public String sID;
    public String caption;
    
    public Class valueClass;
    
    private Map<ReportFieldExtraType, Class> extraTypeClass = new HashMap<>(); 
    
    public void setExtraTypeClass(ReportFieldExtraType type, Class cls) {
        extraTypeClass.put(type, cls);
    }
            
    public Class getExtraTypeClass(ReportFieldExtraType type) {
        return extraTypeClass.get(type);
    }
    
    public Class headerClass;

    public int minimumWidth;
    public int preferredWidth;
    public Integer fixedCharWidth;
    public HorizontalTextAlignEnum alignment;
    public boolean markupHtml;

    public int scale;

    public String pattern;

    // the footer is printed as a value of its own type (its class is the FOOTER extra type's): with its type's markup, and
    // with its type's pattern where it is of another kind of type than the value's, with the value's where it is of the
    // value's kind (a sum)
    public boolean footerMarkupHtml;
    public boolean differentFooter;
    public String footerPattern;

    public String getFooterPattern() {
        return differentFooter ? footerPattern : pattern;
    }

    public boolean hasColumnGroupObjects = false;
    public String columnGroupName;
    
    private Set<ReportFieldExtraType> existingExtras = new HashSet<>();
    
    public void addExtraType(ReportFieldExtraType type) {
        existingExtras.add(type);
    }
    
    public boolean hasExtraType(ReportFieldExtraType type) {
        return existingExtras.contains(type);
    } 
    
    private int charWidth;

    public ReportDrawField(String sID, String caption, int charWidth) {
        this.sID = sID;
        this.caption = caption;
        this.charWidth = charWidth;
    }

    public int getCaptionWidth() {
        return caption.length() * charWidth;
    }

    public int getMinimumWidth() {
        return minimumWidth;
    }

    public Integer widthUser;
    public void setWidthUser(Integer widthUser){
        this.widthUser = widthUser;
    }

    public int getPreferredWidth() {
        int width;
        if (widthUser != null)
            width = widthUser;
        else if (fixedCharWidth != null) {
            width = fixedCharWidth * charWidth;
        } else {
            width = preferredWidth;
        }
        return Math.max(getMinimumWidth(), Math.min(200 * scale, width));
    }

    public void setPreferredWidth(int width) {
        preferredWidth = width;
    }

    public int left;
    public void setLeft(int ileft) {
        left = ileft;
    }

    public int width;
    public void setWidth(int iwidth) {
        width = iwidth;
    }

    public int row;
    public void setRow(int irow) {
        row = irow;
    }
}
