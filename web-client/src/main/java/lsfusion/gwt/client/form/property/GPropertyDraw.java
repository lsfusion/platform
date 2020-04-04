package lsfusion.gwt.client.form.property;

import com.google.gwt.dom.client.Style;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.ImageDescription;
import lsfusion.gwt.client.base.ImageHolder;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.classes.GActionType;
import lsfusion.gwt.client.classes.GClass;
import lsfusion.gwt.client.classes.GObjectType;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.classes.data.GFormatType;
import lsfusion.gwt.client.classes.data.GLongType;
import lsfusion.gwt.client.classes.data.GStringType;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.design.GWidthStringProcessor;
import lsfusion.gwt.client.form.event.GKeyInputEvent;
import lsfusion.gwt.client.form.event.GMouseInputEvent;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.GObject;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.form.property.cell.classes.view.FormatGridCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.GridCellEditor;
import lsfusion.gwt.client.form.property.cell.view.GridCellRenderer;
import lsfusion.gwt.client.form.property.panel.view.PanelRenderer;
import lsfusion.gwt.client.view.MainFrame;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class GPropertyDraw extends GComponent implements GPropertyReader, Serializable {
    public int ID;
    public String sID;
    public String namespace;
    public String caption;
    public String canonicalName;
    public String propertyFormName;

    public String toolTip;
    public String tableName;
    public String[] interfacesCaptions;
    public GClass[] interfacesTypes;
    public String creationScript;
    public String creationPath;
    public String formPath;

    public GGroupObject groupObject;
    public String columnsName;
    public ArrayList<GGroupObject> columnGroupObjects;

    public boolean grid;

    public GType baseType;
    public String pattern;
    public String defaultPattern;
    public GClass returnClass;

    public GType changeWYSType;
    public GType changeType;

    public AddRemove addRemove;
    public boolean askConfirm;
    public String askConfirmMessage;
    
    public boolean hasEditObjectAction;
    public boolean hasChangeAction;

    public GEditBindingMap editBindingMap;

    public ImageHolder imageHolder;
    public boolean focusable;
    public boolean checkEquals;
    public GPropertyEditType editType = GPropertyEditType.EDITABLE;

    public boolean echoSymbols;
    public boolean noSort;
    public GCompare defaultCompare;

    public GKeyInputEvent changeKey;
    public Integer changeKeyPriority;
    public boolean showChangeKey;
    public GMouseInputEvent changeMouse;
    public Integer changeMousePriority;

    public boolean drawAsync;

    public GCaptionReader captionReader;
    public GShowIfReader showIfReader;
    public GFooterReader footerReader;
    public GReadOnlyReader readOnlyReader;
    public GBackgroundReader backgroundReader;
    public GForegroundReader foregroundReader;

    // for pivoting
    public String formula;
    public ArrayList<GPropertyDraw> formulaOperands;

    public String aggrFunc;
    public ArrayList<GLastReader> lastReaders;
    public boolean lastAggrDesc;

    public GPropertyDraw quickFilterProperty;

    public int charWidth;

    public int valueWidth = -1;
    public int valueHeight = -1;

    public boolean panelCaptionAbove;
    
    public boolean columnKeysVertical;
    
    public GFlexAlignment valueAlignment;
    
    public boolean hide;

    private transient GridCellRenderer cellRenderer;
    
    public boolean notNull;

    public static class AddRemove implements Serializable {
        public GObject object;
        public boolean add;

        public AddRemove() {}

        public AddRemove(GObject object, boolean add) {
            this.object = object;
            this.add = add;
        }
    }

    public GPropertyDraw(){}

    public void update(GTableController controller, Map<GGroupObjectValue, Object> values, boolean updateKeys) {
        throw new UnsupportedOperationException();
    }

    public PanelRenderer createPanelRenderer(GFormController form, GGroupObjectValue columnKey) {
        return baseType.createPanelRenderer(form, this, columnKey);
    }

    public GridCellRenderer getGridCellRenderer() {
        if (cellRenderer == null) {
            cellRenderer = baseType.createGridCellRenderer(this);
        }
        return cellRenderer;
    }

    public GridCellEditor createGridCellEditor(EditManager editManager) {
        return baseType.createGridCellEditor(editManager, this);
    }

    public GridCellEditor createValueCellEdtor(EditManager editManager) {
        return baseType.createValueCellEditor(editManager, this);
    }

    public void setUserPattern(String pattern) {
        if(baseType instanceof GFormatType) {
            this.pattern = pattern != null ? pattern : defaultPattern;

            GridCellRenderer renderer = getGridCellRenderer();
            if (renderer instanceof FormatGridCellRenderer) {
                ((FormatGridCellRenderer) renderer).updateFormat();
            } else
                assert false;
        }
    }

    public Object parseChangeValueOrNull(String s) {
        if (s == null || changeWYSType == null) {
            return null;
        }
        try {
            return changeWYSType.parseString(s, pattern);
        } catch (ParseException pe) {
            return null;
        }
    }

    public boolean canUseChangeValueForRendering() {
        return changeType != null && baseType.getClass() == changeType.getClass();
    }

    @Override
    public int getGroupObjectID() {
        return groupObject != null ? groupObject.ID : -1;
    }

    public String getCaptionOrEmpty() {
        return caption == null ? "" : caption;
    }

    public String getDynamicCaption(Object caption) {
        return caption == null ? "" : caption.toString().trim();
    }

    public String getEditCaption(String caption) {
        if (caption == null) {
            caption = this.caption;
        }

        return showChangeKey && changeKey != null ? caption + " (" + changeKey + ")" : caption;
    }

    public String getEditCaption() {
        return getEditCaption(caption);
    }

    public String getNotEmptyCaption() {
        if (caption == null || caption.trim().length() == 0) {
            return getMessages().propertyEmptyCaption();
        } else {
            return caption;
        }
    }

    private static ClientMessages getMessages() {
        return ClientMessages.Instance.get();
    }
    
    public static final String TOOL_TIP_FORMAT =
            "<html><b>%s</b><br>%s";

    public static String getDetailedToolTipFormat() {
        return  "<hr>" +
                "<b>" + getMessages().propertyTooltipCanonicalName() + ":</b> %s<br>" +
                "<b>" + getMessages().propertyTooltipTable() + ":</b> %s<br>" +
                "<b>" + getMessages().propertyTooltipObjects() + ":</b> %s<br>" +
                "<b>" + getMessages().propertyTooltipSignature() + ":</b> %s (%s)<br>" +
                "<b>" + getMessages().propertyTooltipScript() + ":</b> %s<br>" +
                "<b>" + getMessages().propertyTooltipPath() + ":</b> %s<br>" +
                "<hr>" +
                "<b>" + getMessages().propertyTooltipFormPropertyName() + ":</b> %s<br>" +
                "<b>" + getMessages().propertyTooltipFormPropertyDeclaration() + ":</b> %s" +
                "</html>";
    }  
    
    public static String getDetailedActionToolTipFormat() {
        return  "<hr>" +
                "<b>sID:</b> %s<br>" +
                "<b>" + getMessages().propertyTooltipObjects() + ":</b> %s<br>" +
                "<b>" + getMessages().propertyTooltipPath() + ":</b> %s<br>" +
                "<hr>" +
                "<b>" + getMessages().propertyTooltipFormPropertyName() + ":</b> %s<br>" +
                "<b>" + getMessages().propertyTooltipFormPropertyDeclaration() + ":</b> %s" +
                "</html>";
    }
    
    public static String getChangeKeyToolTipFormat() {
        return "<hr><b>" + getMessages().propertyTooltipHotkey() + ":</b> %s<br>";
    }
            
    public String getTooltipText(String caption) {
        String propCaption = GwtSharedUtils.nullTrim(!GwtSharedUtils.isRedundantString(toolTip) ? toolTip : caption);
        String changeKeyText = changeKey == null ? "" : GwtSharedUtils.stringFormat(getChangeKeyToolTipFormat(), changeKey.toString());

        if (!MainFrame.configurationAccessAllowed) {
            return GwtSharedUtils.stringFormat(TOOL_TIP_FORMAT, propCaption, changeKeyText);
        } else {
            String ifaceObjects = GwtSharedUtils.toString(", ", interfacesCaptions);
            String scriptPath = creationPath != null ? creationPath.replace("\n", "<br>") : "";
            String scriptFormPath = formPath != null ? formPath.replace("\n", "<br>") : "";
            
            if (baseType instanceof GActionType) {
                return GwtSharedUtils.stringFormat(TOOL_TIP_FORMAT + getDetailedActionToolTipFormat(),
                        propCaption, changeKeyText, canonicalName, ifaceObjects, scriptPath, propertyFormName, scriptFormPath);
            } else {
                String tableName = this.tableName != null ? this.tableName : "&lt;none&gt;";
                String returnClass = this.returnClass.toString();
                String ifaceClasses = GwtSharedUtils.toString(", ", interfacesTypes);
                String script = creationScript != null ? escapeHTML(creationScript).replace("\n", "<br>") : "";
                
                return GwtSharedUtils.stringFormat(TOOL_TIP_FORMAT + getDetailedToolTipFormat(),
                        propCaption, changeKeyText, canonicalName, tableName, ifaceObjects, returnClass, ifaceClasses,
                        script, scriptPath, propertyFormName, scriptFormPath);
            }
        }
    }

    private String escapeHTML(String value) {
        return value.replace("<", "&lt;").replace(">", "&gt;");
    }

    public ImageDescription getImage() {
        return getImage(true);
    }

    public ImageDescription getImage(boolean enabled) {
        ImageDescription image = imageHolder != null ? imageHolder.getImage() : null;
        if (!enabled && image != null && image.url != null) {
            int dotInd = image.url.lastIndexOf(".");
            if (dotInd != -1) {
                return new ImageDescription(image.url.substring(0, dotInd) + "_Disabled" + image.url.substring(dotInd), image.width, image.height);
            }
        }
        return image;
    } 

    public boolean isReadOnly() {
        return editType == GPropertyEditType.READONLY;
    }

    public boolean isEditableNotNull() {
        return notNull && !isReadOnly();
    }

    public double getFlex() {
        if (flex == -2) {
            return getValueWidth(null);
        }
        return flex;
    }

    public GFlexAlignment getAlignment() {
        return alignment;
    }
    
    public Style.TextAlign getTextAlignStyle() {
        if (valueAlignment != null) {
            switch (valueAlignment) {
                case START:
                    return Style.TextAlign.LEFT;
                case CENTER:
                case STRETCH:
                    return Style.TextAlign.CENTER;
                case END:
                    return Style.TextAlign.RIGHT;
            }
        }
        return null;
    }

    public int getValueWidth(GFont parentFont) {
        return getValueWidth(parentFont, null);
    }

    public int getValueWidth(GFont parentFont, GWidthStringProcessor widthStringProcessor) {
        if (valueWidth != -1) {
            return valueWidth;
        }

        GFont font = this.font != null ? this.font : parentFont;

        String widthString = null;
        if(widthString == null && charWidth != 0)
            widthString = GwtSharedUtils.replicate('0', charWidth);
        if(widthString != null)
            return baseType.getFullWidthString(widthString, font, widthStringProcessor);

        return baseType.getDefaultWidth(font, this, widthStringProcessor);
    }

    public Object getFormat() {
        return (baseType instanceof GObjectType ? GLongType.instance : ((GFormatType)baseType)).getFormat(pattern);
    }

    public int getValueHeight(GFont parentFont) {
        if (valueHeight != -1) {
            return valueHeight;
        }

        int height = baseType.getDefaultHeight(font != null ? font : parentFont);
        final ImageDescription image = getImage();
        if (image != null && image.height >= 0) {
            height = Math.max(image.height + 4, height);
        }
        return height;
    }

    public int getLabelHeight() {
        return new GStringType(new GExtInt(50), false, false).getDefaultHeight(captionFont);
    }

    public LinkedHashMap<String, String> getContextMenuItems() {
        return editBindingMap == null ? null : editBindingMap.getContextMenuItems();
    }

    @Override
    public int hashCode() {
        return ID;
    }

    @Override
    public String toString() {
        return "GPropertyDraw{" +
                "sID='" + sID + '\'' +
                ", caption='" + caption + '\'' +
                ", baseType=" + baseType +
                ", changeType=" + changeType +
                ", imagePath='" + imageHolder.getDefaultImage() + '\'' +
                ", focusable=" + focusable +
                ", checkEquals=" + checkEquals +
                ", editType=" + editType +
                '}';
    }
}
