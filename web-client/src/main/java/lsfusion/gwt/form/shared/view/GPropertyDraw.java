package lsfusion.gwt.form.shared.view;

import lsfusion.gwt.base.shared.GwtSharedUtils;
import lsfusion.gwt.form.client.MainFrame;
import lsfusion.gwt.form.client.form.ui.GFormController;
import lsfusion.gwt.form.shared.view.changes.GGroupObjectValue;
import lsfusion.gwt.form.shared.view.classes.GClass;
import lsfusion.gwt.form.shared.view.classes.GType;
import lsfusion.gwt.form.shared.view.grid.EditManager;
import lsfusion.gwt.form.shared.view.grid.editor.GridCellEditor;
import lsfusion.gwt.form.shared.view.grid.renderer.GridCellRenderer;
import lsfusion.gwt.form.shared.view.logics.GGroupObjectLogicsSupplier;
import lsfusion.gwt.form.shared.view.panel.PanelRenderer;
import lsfusion.gwt.form.shared.view.reader.*;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class GPropertyDraw extends GComponent implements GPropertyReader {
    public int ID;
    public String sID;
    public String caption;

    public String toolTip;
    public String tableName;
    public String[] interfacesCaptions;
    public GClass[] interfacesTypes;
    public String creationScript;
    public String creationPath;

    public GGroupObject groupObject;
    public String columnsName;
    public ArrayList<GGroupObject> columnGroupObjects;

    public GType baseType;
    public GClass returnClass;

    public GType changeWYSType;
    public GType changeType;

    public AddRemove addRemove;
    public boolean askConfirm;
    public String askConfirmMessage;
    
    public boolean hasEditObjectAction;
    public boolean hasChangeAction;

    public GEditBindingMap editBindingMap;

    public ImageDescription icon;
    public boolean focusable;
    public boolean checkEquals;
    public GPropertyEditType editType = GPropertyEditType.EDITABLE;

    public boolean echoSymbols;
    public boolean noSort;

    public GKeyStroke editKey;
    public boolean showEditKey;

    public boolean drawAsync;

    public GCaptionReader captionReader;
    public GShowIfReader showIfReader;
    public GFooterReader footerReader;
    public GReadOnlyReader readOnlyReader;
    public GBackgroundReader backgroundReader;
    public GForegroundReader foregroundReader;

    public GPropertyDraw quickFilterProperty;

    public int minimumCharWidth;
    public int maximumCharWidth;
    public int preferredCharWidth;

    public boolean panelCaptionAbove;
    
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

    public void update(GGroupObjectLogicsSupplier controller, Map<GGroupObjectValue, Object> values, boolean updateKeys) {
        controller.updatePropertyDrawValues(this, values, updateKeys);
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

    public Object parseChangeValueOrNull(String s) {
        if (s == null || changeWYSType == null) {
            return null;
        }
        try {
            return changeWYSType.parseString(s);
        } catch (ParseException pe) {
            return null;
        }
    }

    public Object parseBaseValue(String s) throws ParseException {
        return baseType.parseString(s);
    }

    public boolean canUsePasteValueForRendering() {
        return changeWYSType != null && baseType.getClass() == changeWYSType.getClass();
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

        return showEditKey && editKey != null ? caption + " (" + editKey + ")" : caption;
    }

    public String getEditCaption() {
        return getEditCaption(caption);
    }

    public String getNotEmptyCaption() {
        if (caption == null || caption.trim().length() == 0) {
            return "Неопределённое свойство";
        } else {
            return caption;
        }
    }

    public static final String TOOL_TIP_FORMAT =
            "<html><b>%s</b><br>" +
                    "%s";

    public static final String DETAILED_TOOL_TIP_FORMAT =
            "<hr>" +
            "<b>sID:</b> %s<br>" +
            "<b>Таблица:</b> %s<br>" +
            "<b>Объекты:</b> %s<br>" +
            "<b>Сигнатура:</b> %s <i>%s</i> (%s)<br>" +
            "<b>Скрипт:</b> %s<br>" +
            "<b>Путь:</b> %s" +
            "</html>";

    public static final String EDIT_KEY_TOOL_TIP_FORMAT =
            "<hr><b>Горячая клавиша:</b> %s<br>";

    public String getTooltipText(String caption) {
        String propCaption = GwtSharedUtils.nullTrim(!GwtSharedUtils.isRedundantString(toolTip) ? toolTip : caption);
        String editKeyText = editKey == null ? "" : GwtSharedUtils.stringFormat(EDIT_KEY_TOOL_TIP_FORMAT, editKey.toString());

        if (!MainFrame.configurationAccessAllowed) {
            return GwtSharedUtils.stringFormat(TOOL_TIP_FORMAT, propCaption, editKeyText);
        } else {
            String sid = sID;
            String tableName = this.tableName != null ? this.tableName : "&lt;none&gt;";
            String ifaceObjects = GwtSharedUtils.toString(", ", interfacesCaptions);
            String ifaceClasses = GwtSharedUtils.toString(", ", interfacesTypes);
            String returnClass = this.returnClass.toString();

            String script = creationScript != null ? creationScript.replace("\n", "<br>") : "";
            String scriptPath = creationPath != null ? creationPath.replace("\n", "<br>") : "";
            return GwtSharedUtils.stringFormat(TOOL_TIP_FORMAT + DETAILED_TOOL_TIP_FORMAT, propCaption, editKeyText, sid, tableName, ifaceObjects, returnClass, sid, ifaceClasses, script, scriptPath);
        }
    }

    public String getIconPath(boolean enabled) {
        if (icon == null) {
            return null;
        }
        if (!enabled && icon.url != null) {
            int dotInd = icon.url.lastIndexOf(".");
            if (dotInd != -1) {
                return icon.url.substring(0, dotInd) + "_Disabled" + icon.url.substring(dotInd);
            }
        }

        return icon.url;
    }

    public boolean isReadOnly() {
        return editType == GPropertyEditType.READONLY;
    }

    public boolean isEditableNotNull() {
        return notNull && !isReadOnly();
    }

    public String getMinimumWidth(GFont parentFont) {
        return getMinimumPixelWidth(parentFont) + "px";
    }

    public int getMinimumPixelWidth(GFont parentFont) {
        if (minimumWidth != -1) {
            return minimumWidth;
        } else {
            return baseType.getMinimumPixelWidth(minimumCharWidth, font != null ? font : parentFont);
        }
    }

    public String getMinimumHeight() {
        return getMinimumPixelHeight(null) + "px";
    }

    public int getMinimumPixelHeight(GFont parentFont) {
        if (minimumHeight != -1) {
            return minimumHeight;
        } else {
            return baseType.getMinimumPixelHeight(font != null ? font : parentFont);
        }
    }

    public String getMaximumWidth() {
        return getMaximumPixelWidth() + "px";
    }

    public int getMaximumPixelWidth() {
        if (maximumWidth != -1) {
            return maximumWidth;
        } else {
            return baseType.getMaximumPixelWidth(maximumCharWidth, font);
        }
    }

    public String getMaximumHeight() {
        return maximumHeight + "px";
    }

    public String getPreferredWidth() {
        return getPreferredPixelWidth() + "px";
    }

    public int getPreferredPixelWidth() {
        if (preferredWidth != -1) {
            return preferredWidth;
        } else {
            return baseType.getPreferredPixelWidth(preferredCharWidth, font);
        }
    }

    public String getPreferredHeight() {
        return getPreferredPixelHeight() + "px";
    }

    public int getPreferredPixelHeight() {
        if (preferredHeight != -1) {
            return preferredHeight;
        } else {
            return baseType.getPreferredPixelHeight(font);
        }
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
                ", imagePath='" + icon + '\'' +
                ", focusable=" + focusable +
                ", checkEquals=" + checkEquals +
                ", editType=" + editType +
                '}';
    }
}
