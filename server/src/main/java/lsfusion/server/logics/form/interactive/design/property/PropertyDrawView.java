package lsfusion.server.logics.form.interactive.design.property;

import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.classes.DataType;
import lsfusion.interop.form.event.KeyInputEvent;
import lsfusion.interop.form.event.MouseInputEvent;
import lsfusion.interop.form.print.ReportFieldExtraType;
import lsfusion.interop.form.property.Compare;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.base.AppServerImage;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.TypeSerializer;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.*;
import lsfusion.server.logics.classes.data.integral.IntegerClass;
import lsfusion.server.logics.classes.data.link.LinkClass;
import lsfusion.server.logics.classes.data.time.IntervalClass;
import lsfusion.server.logics.classes.data.time.TimeSeriesClass;
import lsfusion.server.logics.form.interactive.action.async.AsyncEventExec;
import lsfusion.server.logics.form.interactive.action.async.AsyncInput;
import lsfusion.server.logics.form.interactive.action.async.AsyncNoWaitExec;
import lsfusion.server.logics.form.interactive.action.async.AsyncSerializer;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.FormInstanceContext;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.BaseComponentView;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.stat.print.design.ReportDrawField;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.action.ActionObjectEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawExtraType;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.logics.form.struct.property.oraction.ActionOrPropertyObjectEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.exec.db.table.MapKeysTable;

import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.pow;
import static java.lang.Math.round;
import static lsfusion.base.BaseUtils.nvl;
import static lsfusion.interop.action.ServerResponse.CHANGE;
import static lsfusion.interop.action.ServerResponse.EDIT_OBJECT;
import static lsfusion.server.logics.form.struct.property.PropertyDrawExtraType.*;

public class PropertyDrawView extends BaseComponentView {

    public PropertyDrawEntity<?> entity;

    public Boolean changeOnSingleClick;
    public boolean hide;
    public Long maxValue;
    public Boolean echoSymbols;
    public boolean noSort;
    public Compare defaultCompare;

    public int charWidth;
    private int charHeight;

    public Dimension valueSize;
    public Integer valueWidth;
    public Integer valueHeight;

    public Integer captionWidth;
    public Integer captionHeight;

    private Boolean valueFlex;

    public String tag;
    public String inputType;
    public String valueElementClass;
    public String captionElementClass;

    public KeyInputEvent changeKey;
    public Integer changeKeyPriority;
    public Boolean showChangeKey;
    public MouseInputEvent changeMouse;
    public Integer changeMousePriority;
    public Boolean showChangeMouse;

    public boolean drawAsync = false;

    public Boolean inline;

    public Boolean focusable;

    public boolean panelColumnVertical = false;

    public FlexAlignment valueAlignmentHorz;
    public FlexAlignment valueAlignmentVert;

    public String valueOverflowHorz;
    public String valueOverflowVert;

    public Boolean valueShrinkHorz;
    public Boolean valueShrinkVert;

    public LocalizedString comment;
    public String commentElementClass;
    public boolean panelCommentVertical;
    public Boolean panelCommentFirst;
    public FlexAlignment panelCommentAlignment;

    public LocalizedString placeholder;
    public LocalizedString pattern;
    public LocalizedString regexp;
    public LocalizedString regexpMessage;

    public LocalizedString tooltip;
    public LocalizedString valueTooltip;

    public LocalizedString caption;
    public AppServerImage.Reader image;

    public Integer wrap;
    public Boolean wrapWordBreak;
    public Boolean collapse;
    public Boolean ellipsis;

    public Integer captionWrap;
    public Boolean captionWrapWordBreak;
    public Boolean captionCollapse;
    public Boolean captionEllipsis;

    public boolean clearText;
    public boolean notSelectAll;

    public Boolean toolbar;
    public Boolean toolbarActions;

    public boolean notNull;

    public Boolean sticky;
    public Boolean sync;

    @SuppressWarnings({"UnusedDeclaration"})
    public PropertyDrawView() {

    }

    public PropertyDrawView(PropertyDrawEntity entity) {
        super(entity.ID);
        this.entity = entity;
        setMargin(2);
        setSID("PROPERTY(" + entity.getSID() + ")");
    }
    
    public String getPropertyFormName() {
        return entity.getSID();
    }

    public String getIntegrationSID() {
        return entity.getIntegrationSID();
    }

    public Type getAssertCellType(FormInstanceContext context) {
        return entity.getAssertCellType(context);
    }

    public Type getAssertValueType(FormInstanceContext context) {
        return entity.getAssertValueProperty(context).getType();
    }

    public boolean isDifferentValue(FormInstanceContext context) {
        return entity.isDifferentValue(context);
    }

    public Type getFilterType(FormInstanceContext context) {
        return entity.getFilterProperty(context).getType();
    }

    public boolean isProperty(FormInstanceContext context) {
        return entity.isProperty(context);
    }

    public int getValueWidth(FormInstanceContext context) {
        if(valueWidth != null)
            return valueWidth;

        if(isCustom(context) || !isProperty(context))
            return -1;

        return -2;
    }

    public static final boolean moreInfo = false;

    @Override
    protected boolean isDefaultCaptionVertical(FormInstanceContext context) {
        return false;
    }

    private boolean isPanelBoolean(FormInstanceContext context) {
        return isProperty(context) && getAssertCellType(context) instanceof LogicalClass;
    }

    @Override
    protected boolean isDefaultCaptionLast(FormInstanceContext context) {
        // the main problem here is that in the boolean default caption last we don't know if it's gonna be aligned, so we'll use that hack in PropertyPanelRenderer.initCaption for now (until we'll move isAlignCaptions to the server)
        return isPanelBoolean(context) && !isCaptionVertical(context);
    }

    @Override
    protected FlexAlignment getDefaultCaptionAlignmentHorz(FormInstanceContext context) {
        // not sure that this is needed if we decide to maintain grid horz alignments to start
//        if(!entity.isList(context) && isCaptionVertical(context))
//            return FlexAlignment.CENTER;

        if(entity.isList(context)) {
            if (isProperty(context)) {
                Type type = getAssertValueType(context);
                // if we have "fixed size type" we want also start caption alignment
                if (type instanceof TimeSeriesClass || type instanceof IntervalClass)
                    return FlexAlignment.START;
            }

            return getValueAlignmentHorz(context);
        }

        return FlexAlignment.START;
    }

    @Override
    protected FlexAlignment getDefaultCaptionAlignmentVert(FormInstanceContext context) {
//        if(entity.isList(context))
//            return FlexAlignment.END;

        return FlexAlignment.CENTER;
    }

    protected boolean isPanelCommentFirst(FormInstanceContext context) {
        if(panelCommentFirst != null)
            return panelCommentFirst;

        return false;
    }

    protected FlexAlignment getPanelCommentAlignment(FormInstanceContext context) {
        if(panelCommentAlignment != null)
            return panelCommentAlignment;

        return FlexAlignment.CENTER;
    }

    public int getValueHeight(FormInstanceContext context) {
        if(valueHeight != null)
            return valueHeight;

        if(isCustom(context) || (!isProperty(context) && !entity.hasDynamicImage())) // we want vertical size for action to be equal to text fields
            return -1;

        return -2;
    }

    public int getCaptionWidth(FormEntity entity) {
        if(captionWidth != null)
            return captionWidth;

        return -1;
    }

    public int getCaptionHeight(FormInstanceContext context) {
        if(captionHeight != null)
            return captionHeight;

        return moreInfo && entity.isList(context) ? 54 : -1; // we need fixed height to set wrap -1 to avoid ellipsis
    }

    public int getWrap(FormInstanceContext context) {
        if (wrap != null)
            return wrap;

        if (isProperty(context)) {
            Type type = getAssertCellType(context);
            if (type instanceof TextClass)
                return -1;

            return context.contentWordWrap ? -1 : 1;
        }

        return 1;
    }

    public int getCaptionWrap(FormInstanceContext context) {
        if (captionWrap != null)
            return captionWrap;

        if(entity.isList(context))
            return moreInfo ? -1 : 3;
        else
            return isCaptionVertical(context) ? -1 : 1;
    }

    public boolean isCollapse(FormInstanceContext context) {
        if (collapse != null)
            return collapse;

        if (isProperty(context)) {
            Type type = getAssertCellType(context);
            if (type instanceof TextClass)
                return false;
        }

        return true;
    }

    public boolean isCaptionCollapse(FormInstanceContext context) {
        if (captionCollapse != null)
            return captionCollapse;

        return false;
    }

    public boolean isWrapWordBreak(FormInstanceContext context) {
        if (wrapWordBreak != null)
            return wrapWordBreak;

        return moreInfo;
    }

    public boolean isCaptionWrapWordBreak(FormInstanceContext context) {
        if (captionWrapWordBreak != null)
            return captionWrapWordBreak;

        return false; // moreInfo; false looks odd with wrap 2, 3 // && entity.isList(context);
    }

    private static final ExtInt ELLIPSIS_LIMIT = new ExtInt(40);

    public boolean isEllipsis(FormInstanceContext context) {
        if (ellipsis != null)
            return ellipsis;

        if (!moreInfo && isProperty(context)) {
            Type type = getAssertCellType(context);
            if (type instanceof StringClass && ELLIPSIS_LIMIT.less(type.getCharLength()))
                return true;
        }

        return false;
    }

    public boolean isCaptionEllipsis(FormInstanceContext context) {
        if (captionEllipsis != null)
            return captionEllipsis;

        return !moreInfo;
    }

    // we force optimistic async event scheme for external calls (since this calls assume that async push should exist)
    // for that purpose we have to send to client that type to do parsing, rendering, etc.
    public Type getExternalChangeType(FormInstanceContext context) {
        return getChangeType(context, true);
    }

    public Type getChangeType(FormInstanceContext context, boolean externalChange) {
        AsyncEventExec asyncEventExec = entity.getAsyncEventExec(context, CHANGE, externalChange);
        return asyncEventExec instanceof AsyncInput ? ((AsyncInput) asyncEventExec).changeType : null;
    }

    @Override
    public double getDefaultFlex(FormInstanceContext context) {
        ContainerView container = getLayoutParamContainer();
        boolean isList = entity.isList(context);
        if(((container != null && container.isHorizontal()) || isList) && isHorizontalValueFlex(context))
            return -2; // flex = width
        if(isList && Settings.get().isDefaultFlexInGrid())
            return 0.001;
        return super.getDefaultFlex(context);
    }

    @Override
    protected boolean isDefaultShrink(FormInstanceContext context, boolean explicit) {
        ContainerView container = getLayoutParamContainer();
        if(container != null && container.isHorizontal() && container.isWrap() && isHorizontalValueShrink(context))
            return true;
        return super.isDefaultShrink(context, explicit);
    }

    @Override
    public FlexAlignment getDefaultAlignment(FormInstanceContext context) {
        ContainerView container = getLayoutParamContainer();
        if (container != null && !container.isHorizontal() && isHorizontalValueFlex(context))
            return FlexAlignment.STRETCH;
        return super.getDefaultAlignment(context);
    }

    @Override
    protected boolean isDefaultAlignShrink(FormInstanceContext context, boolean explicit) {
        // actually not needed mostly since for STRETCH align shrink is set, but just in case
        ContainerView container = getLayoutParamContainer();
        if (container != null && !container.isHorizontal() && isHorizontalValueShrink(context))
            return true;
        return super.isDefaultAlignShrink(context, explicit);
    }

    private String getCustomRenderFunction(FormInstanceContext context) {
        return entity.getCustomRenderFunction(context);
    }
    private boolean isCustomCanBeRenderedInTD(FormInstanceContext context) {
        return entity.isCustomCanBeRenderedInTD(context);
    }
    private boolean isCustomNeedPlaceholder(FormInstanceContext context) {
        return entity.isCustomNeedPlaceholder(context);
    }
    private boolean isCustomNeedReadonly(FormInstanceContext context) {
        return entity.isCustomNeedReadonly(context);
    }

    private LocalizedString getPlaceholder(FormInstanceContext context) {
        if(placeholder != null)
            return placeholder;

        if (isProperty(context)) {
            String tag = getTag(context);
            if (tag != null && tag.equals("a") && getAssertValueType(context) instanceof StringClass)
                return LocalizedString.create("{form.renderer.not.defined}");
        }

        return null;
    }

    public static final boolean defaultSync = true;

    public Map<String, AsyncEventExec> getAsyncEventExec(FormInstanceContext context) {
        Map<String, AsyncEventExec> asyncExecMap = new HashMap<>();
        Boolean sync = getSync();
        if(sync == null || !sync) { // if WAIT we don't want any asyncs
            for (String actionId : entity.getAllPropertyEventActions(context)) {
                AsyncEventExec asyncEventExec = entity.getAsyncEventExec(context, actionId, false);
                if (asyncEventExec == null && (sync != null || !defaultSync)) // explicit NOWAIT or not default sync
                    asyncEventExec = AsyncNoWaitExec.instance;
                if (asyncEventExec != null)
                    asyncExecMap.put(actionId, asyncEventExec);
            }
        }
        return asyncExecMap;
    }

    public LocalizedString getCaption() {
        return caption != null
                ? caption
                : entity.getCaption();
    }

    public AppServerImage.AutoName getAutoName() {
        return AppServerImage.getAutoName(this::getCaption, entity.getInheritedProperty()::getName);
    }

    public void setImage(String image) {
        this.image = AppServerImage.createPropertyImage(image, this);
    }

    public AppServerImage getImage(ConnectionContext context) {
        if(this.image != null)
            return this.image.get(context);

        AppServerImage.Reader entityImage = entity.getImage();
        if(entityImage != null)
            return entityImage.get(context);

        return getDefaultImage(context);
    }

    private AppServerImage getDefaultImage(ConnectionContext context) {
        return ActionOrProperty.getDefaultImage(AppServerImage.AUTO, getAutoName(), Settings.get().getDefaultPropertyImageRankingThreshold(), Settings.get().isDefaultPropertyImage(), context);
    }

    // we return to the client null, if we're sure that caption is always empty (so we don't need to draw label)
    public String getDrawCaption() {
        LocalizedString caption = getCaption();
        if(hasNoCaption(caption, entity.getPropertyExtra(CAPTION), elementClass))
            return null;

        return ThreadLocalContext.localize(caption);
    }

    public static boolean hasNoCaption(LocalizedString caption, PropertyObjectEntity<?> propertyCaption, String elementClass) {
        return ((caption == null || (caption.isEmpty() && elementClass == null)) && propertyCaption == null) || (propertyCaption != null && propertyCaption.property.isExplicitNull()); // isEmpty can be better, but we just want to emulate NULL to be like NULL caption
    }

    public boolean isNotNull() {
        return notNull || entity.isNotNull();
    }

    public boolean isSticky(FormInstanceContext context) {
        if (entity.sticky != null)
            return entity.sticky;

        if (sticky != null)
            return sticky;

        if(!isProperty(context))
            return false;

        if(entity.getAssertValueProperty(context).isValueUnique(entity.getToDraw(context.entity), Property.ValueUniqueType.STICKY))
            return true;

        if(ThreadLocalContext.getBaseLM().isRecognize(entity.getInheritedProperty()))
            return true;

        return false;
    }

    public Boolean getSync() {
        return nvl(entity.sync, sync);
    }

    //Для Jasper'а экранируем кавычки
    public String getReportCaption() {
        LocalizedString caption = getCaption();
        return caption == null ? null : ThreadLocalContext.localize(caption).replace("\"", "\\\"");
    }

    public ReportDrawField getReportDrawField(int charWidth, int scale, Type type) {
        ReportDrawField reportField = new ReportDrawField(getPropertyFormName(), getReportCaption(), charWidth);

        setupGeometry(reportField, scale);
        setupColumnGroupObjects(reportField);

        PropertyDrawExtraType[] setupTypes = {CAPTION, FOOTER, BACKGROUND, FOREGROUND, IMAGE};
        for (PropertyDrawExtraType setupType : setupTypes) {
            setupExtra(reportField, setupType);
        }
        setupShowIf(reportField);

        type.fillReportDrawField(reportField);

        String reportPattern = getReportPattern();
        if(reportPattern != null)
            reportField.pattern = reportPattern;

        return reportField;
    }

    private String getReportPattern() {
        return pattern != null ? pattern.getSourceString() : null;
    }

    private void setupGeometry(ReportDrawField reportField, int scale) {
        reportField.scale = scale;

        if(entity.isStaticProperty()) {
            Type type = entity.getStaticType();
            reportField.minimumWidth = type.getReportMinimumWidth() * scale;
            reportField.preferredWidth = type.getReportPreferredWidth() * scale;
        }
        int reportCharWidth = charWidth;
        if (reportCharWidth != 0) {
            reportField.fixedCharWidth = reportCharWidth * scale;
        }
    }

    private void setupColumnGroupObjects(ReportDrawField reportField) {
        if (!entity.getColumnGroupObjects().isEmpty()) {
            reportField.hasColumnGroupObjects = true;
            reportField.columnGroupName = entity.columnsName;
        }
    }

    private void setupExtra(ReportDrawField field, PropertyDrawExtraType type) {
        ReportFieldExtraType reportType = type.getReportExtraType();
        if (entity.hasPropertyExtra(type)) {
            field.addExtraType(reportType);
            field.setExtraTypeClass(reportType, getPropertyClass(entity.getPropertyExtra(type)));
        } else {
            field.setExtraTypeClass(reportType, String.class);            
        }
    }
    
    private void setupShowIf(ReportDrawField field) {
        if (entity.hasPropertyExtra(PropertyDrawExtraType.SHOWIF)) {
            field.addExtraType(ReportFieldExtraType.SHOWIF);
            field.setExtraTypeClass(ReportFieldExtraType.SHOWIF, getPropertyClass(entity.getPropertyExtra(PropertyDrawExtraType.SHOWIF)));
        }
    }

    private Class getPropertyClass(PropertyObjectEntity<?> property) {
        ReportDrawField field = new ReportDrawField("", "", charWidth);
        Type type = property.property.getType();
        if (type != null) {
            type.fillReportDrawField(field);
        }
        return field.valueClass;
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        super.customSerialize(pool, outStream);

        pool.writeString(outStream, getDrawCaption());
        AppServerImage.serialize(getImage(pool.context), outStream, pool);
        pool.writeLong(outStream, maxValue);
        outStream.writeBoolean(echoSymbols);
        outStream.writeBoolean(noSort);

        Compare defaultCompare = getDefaultCompare(pool.context);
        if(defaultCompare != null)
            defaultCompare.serialize(outStream);
        else
            outStream.writeByte(-1);

        outStream.writeInt(getCharHeight());
        outStream.writeInt(getCharWidth(pool.context));

        outStream.writeInt(getValueWidth(pool.context));
        outStream.writeInt(getValueHeight(pool.context));

        outStream.writeInt(getCaptionWidth(pool.context.entity));
        outStream.writeInt(getCaptionHeight(pool.context));

        pool.writeObject(outStream, changeKey);
        pool.writeInt(outStream, changeKeyPriority);
        outStream.writeBoolean(showChangeKey);
        pool.writeObject(outStream, changeMouse);
        pool.writeInt(outStream, changeMousePriority);
        outStream.writeBoolean(showChangeMouse);

        outStream.writeBoolean(drawAsync);

        pool.writeObject(outStream, inline);
        outStream.writeBoolean(entity.isList(pool.context));

        pool.writeObject(outStream, focusable);
        outStream.writeByte(entity.getEditType().serialize());

        outStream.writeBoolean(panelColumnVertical);
        
        pool.writeObject(outStream, getValueAlignmentHorz(pool.context));
        pool.writeObject(outStream, getValueAlignmentVert(pool.context));

        pool.writeString(outStream, getValueOverflowHorz(pool.context));
        pool.writeString(outStream, getValueOverflowVert(pool.context));

        pool.writeBoolean(outStream, getValueShrinkHorz(pool.context));
        pool.writeBoolean(outStream, getValueShrinkVert(pool.context));

        pool.writeString(outStream, ThreadLocalContext.localize(comment));
        pool.writeString(outStream, getCommentElementClass(pool.context));
        outStream.writeBoolean(panelCommentVertical);
        outStream.writeBoolean(isPanelCommentFirst(pool.context));
        pool.writeObject(outStream, getPanelCommentAlignment(pool.context));

        pool.writeString(outStream, ThreadLocalContext.localize(getPlaceholder(pool.context)));
        pool.writeString(outStream, ThreadLocalContext.localize(getPattern(pool.context)));
        pool.writeString(outStream, ThreadLocalContext.localize(regexp));
        pool.writeString(outStream, ThreadLocalContext.localize(regexpMessage));

        pool.writeString(outStream, ThreadLocalContext.localize(tooltip));
        pool.writeString(outStream, ThreadLocalContext.localize(valueTooltip));

        pool.writeObject(outStream, getChangeOnSingleClick(pool.context));
        outStream.writeBoolean(hide);

        //entity часть
        if(isProperty(pool.context)) {
            Type cellType = getAssertCellType(pool.context);
            // however this hack helps only in some rare cases, since baseType is used in a lot of places
            if(cellType == null) // temporary hack, will not be needed when expression will be automatically patched with "IS Class"
                cellType = IntegerClass.instance;
            TypeSerializer.serializeType(outStream, cellType);
        } else {
            outStream.writeByte(1);
            outStream.writeByte(DataType.ACTION);
        }

        // optimization
        boolean differentValue = isDifferentValue(pool.context);
        outStream.writeBoolean(differentValue);
        if(differentValue)
            TypeSerializer.serializeType(outStream, getAssertValueType(pool.context));

        pool.writeString(outStream, getTag(pool.context));
        pool.writeString(outStream, getInputType(pool.context));
        pool.writeString(outStream, getValueElementClass(pool.context));
        pool.writeString(outStream, getCaptionElementClass(pool.context));
        pool.writeBoolean(outStream, hasToolbar(pool.context));
        pool.writeBoolean(outStream, hasToolbarActions(pool.context));

        Type externalChangeType = getExternalChangeType(pool.context);
        outStream.writeBoolean(externalChangeType != null);
        if (externalChangeType != null) {
            TypeSerializer.serializeType(outStream, externalChangeType);
        }

        Map<String, AsyncEventExec> asyncExecMap = getAsyncEventExec(pool.context);
        outStream.writeInt(asyncExecMap.size());
        for (Map.Entry<String, AsyncEventExec> entry : asyncExecMap.entrySet()) {
            pool.writeString(outStream, entry.getKey());
            AsyncSerializer.serializeEventExec(entry.getValue(), pool.context, outStream);
        }

        outStream.writeBoolean(entity.ignoreHasHeaders);

        outStream.writeBoolean(entity.askConfirm);
        if(entity.askConfirm)
            pool.writeString(outStream, getAskConfirmMessage(pool.context));
        outStream.writeBoolean(hasEditObjectAction(pool.context));
        outStream.writeBoolean(hasChangeAction(pool.context));
        outStream.writeBoolean(entity.hasDynamicImage());
        outStream.writeBoolean(entity.hasDynamicCaption());

        ActionOrProperty inheritedProperty = entity.getInheritedProperty();
        outStream.writeBoolean(inheritedProperty instanceof Property && ((Property<?>) inheritedProperty).disableInputList);

        ActionOrPropertyObjectEntity<?, ?> debug = entity.getReflectionActionOrProperty(); // only for tooltip
        ActionOrProperty<?> debugBinding = entity.getReflectionBindingProperty(); // only for tooltip

        pool.writeString(outStream, debugBinding.getNamespace());
        pool.writeString(outStream, getSID());
        pool.writeString(outStream, debugBinding.getCanonicalName());
        pool.writeString(outStream, getPropertyFormName());
        pool.writeString(outStream, getIntegrationSID());
        pool.serializeObject(outStream, pool.context.view.getGroupObject(entity.getToDraw(pool.context.entity)));

        pool.writeString(outStream, entity.columnsName);
        ImOrderSet<GroupObjectEntity> columnGroupObjects = entity.getColumnGroupObjects();
        outStream.writeInt(columnGroupObjects.size());
        for (GroupObjectEntity groupEntity : columnGroupObjects) {
            pool.serializeObject(outStream, pool.context.view.getGroupObject(groupEntity));
        }

        outStream.writeBoolean(isProperty(pool.context));

        outStream.writeInt(getWrap(pool.context));
        outStream.writeBoolean(isWrapWordBreak(pool.context));
        outStream.writeBoolean(isCollapse(pool.context));
        outStream.writeBoolean(isEllipsis(pool.context));

        outStream.writeInt(getCaptionWrap(pool.context));
        outStream.writeBoolean(isCaptionWrapWordBreak(pool.context));
        outStream.writeBoolean(isCaptionCollapse(pool.context));
        outStream.writeBoolean(isCaptionEllipsis(pool.context));

        outStream.writeBoolean(clearText);
        outStream.writeBoolean(notSelectAll);

        // for pivoting
        pool.writeString(outStream, entity.formula);
        if(entity.formula != null) {
            ImList<PropertyDrawEntity> formulaOperands = entity.formulaOperands;
            outStream.writeInt(formulaOperands.size());
            for (PropertyDrawEntity formulaOperand : formulaOperands)
                pool.serializeObject(outStream, pool.context.view.get(formulaOperand));
        }

        pool.writeString(outStream, entity.aggrFunc != null ? entity.aggrFunc.toString() : null);
        outStream.writeInt(entity.lastAggrColumns.size());
        outStream.writeBoolean(entity.lastAggrDesc);

        pool.serializeObject(outStream, pool.context.view.get(entity.quickFilterProperty));

        MapKeysTable<? extends PropertyInterface> mapTable = isProperty(pool.context) ?
                        ((Property<?>)debugBinding).mapTable : null;
        pool.writeString(outStream, mapTable != null ? mapTable.table.getName() : null);

        ImMap<PropertyInterface, ValueClass> interfaceClasses = (ImMap<PropertyInterface, ValueClass>) debug.property.getInterfaceClasses(ClassType.formPolicy);
        ImMap<PropertyInterface, ObjectEntity> interfaceEntities = (ImMap<PropertyInterface, ObjectEntity>) debug.mapping;
        outStream.writeInt(debug.property.interfaces.size());
        for (PropertyInterface iFace : debug.property.interfaces) {
            pool.writeString(outStream, interfaceEntities.get(iFace).toString());
            
            ValueClass paramClass = interfaceClasses.get(iFace);
            outStream.writeBoolean(paramClass != null);
            if (paramClass != null) {
                paramClass.serialize(outStream);
            }
        }

        if(isProperty(pool.context)) {
            ValueClass valueClass = ((PropertyObjectEntity<?>) debug).property.getValueClass(ClassType.formPolicy);
            outStream.writeBoolean(valueClass != null);
            if(valueClass != null)
                valueClass.serialize(outStream);
        } else {
            outStream.writeBoolean(true);
            outStream.writeByte(DataType.ACTION);
        }
        
        pool.writeString(outStream, getCustomRenderFunction(pool.context));
        pool.writeBoolean(outStream, isCustomCanBeRenderedInTD(pool.context));
        pool.writeBoolean(outStream, isCustomNeedPlaceholder(pool.context));
        pool.writeBoolean(outStream, isCustomNeedReadonly(pool.context));

        pool.writeString(outStream, entity.eventID);

        pool.writeString(outStream, debug.getCreationScript());
        pool.writeString(outStream, debug.getCreationPath());
        pool.writeString(outStream, debug.getPath());
        pool.writeString(outStream, entity.getFormPath());

        pool.writeString(outStream, entity.getMouseBinding(pool.context));

        ImMap<KeyStroke, String> keyBindings = entity.getKeyBindings(pool.context);
        outStream.writeInt(keyBindings == null ? 0 : keyBindings.size());
        if (keyBindings != null) {
            for (int i=0,size=keyBindings.size();i<size;i++) {
                pool.writeObject(outStream, keyBindings.getKey(i));
                pool.writeString(outStream, keyBindings.getValue(i));
            }
        }

        OrderedMap<String, LocalizedString> contextMenuBindings = filterContextMenuItems(entity.getContextMenuBindings(pool.context), pool.context);
        outStream.writeInt(contextMenuBindings == null ? 0 : contextMenuBindings.size());
        if (contextMenuBindings != null) {
            for (int i = 0; i < contextMenuBindings.size(); ++i) {
                pool.writeString(outStream, contextMenuBindings.getKey(i));
                pool.writeString(outStream, ThreadLocalContext.localize(contextMenuBindings.getValue(i)));
            }
        }

        outStream.writeBoolean(isNotNull());
        outStream.writeBoolean(isSticky(pool.context));
        outStream.writeBoolean(entity.getPropertyExtra(PropertyDrawExtraType.FOOTER) != null);
    }

    private OrderedMap<String, LocalizedString> filterContextMenuItems(OrderedMap<String, LocalizedString> contextMenuBindings, FormInstanceContext context) {
        if (contextMenuBindings == null || contextMenuBindings.size() == 0) {
            return null;
        }

        OrderedMap<String, LocalizedString> contextMenuItems = new OrderedMap<>();
        for (int i = 0; i < contextMenuBindings.size(); ++i) {
            String actionSID = contextMenuBindings.getKey(i);
            LocalizedString caption = contextMenuBindings.getValue(i);
            ActionObjectEntity<?> eventAction = entity.getCheckedEventAction(actionSID, context);
            if (eventAction != null && context.securityPolicy.checkPropertyViewPermission(eventAction.property)) {
                contextMenuItems.put(actionSID, caption);
            }
        }
        return contextMenuItems;
    }

    private LocalizedString getPattern(FormInstanceContext context) {
        if(pattern != null)
            return pattern;

        if (isProperty(context)) {
            String inputType = getInputType(context);
            if (inputType != null && inputType.equals("year"))
                return LocalizedString.create("{####}");
        }

        return null;
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        boxed = inStream.readBoolean();

        caption = LocalizedString.create(pool.readString(inStream));
        maxValue = pool.readLong(inStream);
        echoSymbols = inStream.readBoolean();
        noSort = inStream.readBoolean();
        defaultCompare = Compare.deserialize(inStream);

        setCharHeight(inStream.readInt());
        setCharWidth(inStream.readInt());
        setValueSize(pool.readObject(inStream));

        setCaptionWidth(inStream.readInt());
        setCaptionHeight(inStream.readInt());

        changeKey = pool.readObject(inStream);
        changeKeyPriority = pool.readInt(inStream);
        showChangeKey = inStream.readBoolean();
        changeMouse = pool.readObject(inStream);
        changeMousePriority = pool.readInt(inStream);
        showChangeMouse = inStream.readBoolean();

        focusable = pool.readObject(inStream);

        panelColumnVertical = inStream.readBoolean();

        valueAlignmentHorz = pool.readObject(inStream);
        valueAlignmentVert = pool.readObject(inStream);

        valueOverflowHorz = pool.readString(inStream);
        valueOverflowVert = pool.readString(inStream);

        valueShrinkHorz = pool.readBoolean(inStream);
        valueShrinkVert = pool.readBoolean(inStream);

        changeOnSingleClick = pool.readObject(inStream);
        hide = inStream.readBoolean();

        entity = pool.context.entity.getPropertyDraw(inStream.readInt());
    }

    @Override
    public String toString() {
        return ThreadLocalContext.localize(getCaption()) + " " + super.toString();
    }

    public Compare getDefaultCompare(FormInstanceContext context) {
        if(defaultCompare != null)
            return defaultCompare;

        if(isProperty(context)) {
            Type type = getFilterType(context);
            if (type != null) {
                return type.getDefaultCompare();
            }
        }

        return null;
    }

    public int getCharHeight() {
        return charHeight;
    }

    public void setCharHeight(int charHeight) {
        this.charHeight = charHeight;
    }

    public int getCharWidth(FormInstanceContext context) {
        PropertyDrawEntity.Select select = entity.getSelectProperty(context);
        if(select != null) {
            if(select.elementType.equals("Input") || (select.elementType.equals("Dropdown") && select.type.equals("Multi")))
                return getScaledCharWidth((charWidth != 0 ? charWidth : select.length / select.count) * 4);

            if (!entity.isList(context))
                return 0;

            if (select.elementType.startsWith("Button"))
                return (charWidth != 0 && !select.actual ? charWidth * select.count : select.length) + select.count * (select.elementType.startsWith("ButtonGroup") ? 4 : 6); // couple of symbols for padding
        }

        return charWidth;
    }

    // the same is on the client
    private static int getScaledCharWidth(int lengthValue) {
        return lengthValue <= 12 ? Math.max(lengthValue, 1) : (int) round(12 + pow(lengthValue - 12, 0.7));
    }


    public void setCharWidth(int minimumCharWidth) {
        this.charWidth = minimumCharWidth;
    }

    public Dimension getValueSize() {
        return valueSize;
    }

    public void setValueSize(Dimension valueSize) {
        this.valueWidth = valueSize.width;
        this.valueHeight = valueSize.height;
    }

    public void setValueWidth(Integer valueWidth) {
        this.valueWidth = valueWidth;
    }

    public void setValueHeight(Integer valueHeight) {
        this.valueHeight = valueHeight;
    }

    public void setCaptionWidth(Integer captionWidth) {
        this.captionWidth = captionWidth;
    }

    public void setCaptionHeight(Integer captionHeight) {
        this.captionHeight = captionHeight;
    }

    public Boolean getValueFlex() {
        return valueFlex;
    }

    public void setValueFlex(Boolean valueFlex) {
        this.valueFlex = valueFlex;
    }

    public boolean isHorizontalValueFlex(FormInstanceContext context) {
        if(valueFlex != null)
            return valueFlex;
        Type type;
        return isProperty(context) && (type = getAssertCellType(context)) != null && type.isFlex();
    }

    public boolean isHorizontalValueShrink(FormInstanceContext context) {
//        if(valueFlex != null)
//            return valueFlex;
        Type type;
        return isProperty(context) && (type = getAssertCellType(context)) != null && type.isFlex();
    }

    public boolean isDefaultShrinkOverflowVisible(FormInstanceContext context) {
        return !entity.isList(context) && (isTagInput(context) || entity.getSelectProperty(context) != null); // inputs (incl. multi) + radio + select use shadows
    }

    public String getAskConfirmMessage(FormInstanceContext context) {
        assert entity.askConfirm;
        if (entity.askConfirmMessage != null)
            return entity.askConfirmMessage;
        
        LocalizedString msg;
        if (isProperty(context)) {
            msg = LocalizedString.create("{form.instance.do.you.really.want.to.edit.property}");
        } else {
            msg = LocalizedString.create("{form.instance.do.you.really.want.to.take.action}");
        }
        LocalizedString caption = getCaption();
        if (!caption.isEmpty()) {
            msg = LocalizedString.concatList(msg, " \"", caption, "\"?");
        }

        return ThreadLocalContext.localize(msg);
    }
    
    public boolean hasChangeAction(FormInstanceContext context) {
        return hasAction(context, CHANGE);
    }
    public boolean hasEditObjectAction(FormInstanceContext context) {
        return hasAction(context, EDIT_OBJECT);
    }

    public boolean hasAction(FormInstanceContext context, String actionID) {
        ActionObjectEntity<?> eventAction = entity.getCheckedEventAction(actionID, context);
        if(eventAction != null)
            return eventAction.property.hasFlow(ChangeFlowType.ANYEFFECT);
        return false;
    }
    public boolean hasFlow(FormInstanceContext context, ChangeFlowType type) {
        ActionObjectEntity<?> eventAction = entity.getCheckedEventAction(CHANGE, context);
        if(eventAction != null)
            return eventAction.property.hasFlow(type);
        return false;
    }

    public Boolean getChangeOnSingleClick(FormInstanceContext context) {
        if(changeOnSingleClick != null)
            return changeOnSingleClick;

        if(isProperty(context)) {
            if (getAssertCellType(context) instanceof LogicalClass)
                return Settings.get().getChangeBooleanOnSingleClick();
        } else
            return Settings.get().getChangeActionOnSingleClick();

        return null;
    }

    public FlexAlignment getValueAlignmentHorz(FormInstanceContext context) {
        if(valueAlignmentHorz != null)
            return valueAlignmentHorz;

        if (isProperty(context)) {
            Type type = getAssertValueType(context);
            if (type != null)
                return type.getValueAlignmentHorz();
            return FlexAlignment.START;
        }

        return FlexAlignment.CENTER;
    }

    public FlexAlignment getValueAlignmentVert(FormInstanceContext context) {
        if (valueAlignmentVert != null)
            return valueAlignmentVert;

        if(isProperty(context)) {
            Type type = getAssertValueType(context);
            if (type != null)
                return type.getValueAlignmentVert();
        }

        return FlexAlignment.CENTER;
    }

    public String getValueOverflowHorz(FormInstanceContext context) {
        if(valueOverflowHorz != null)
            return valueOverflowHorz;

        if(isShrinkOverflowVisible(context))
            return "visible";

        if(isProperty(context)) {
            Type type = getAssertValueType(context);
            if (type != null)
                return type.getValueOverflowHorz();
        }

        return "clip";
    }

    public String getValueOverflowVert(FormInstanceContext context) {
        if(valueOverflowVert != null)
            return valueOverflowVert;

        if(isShrinkOverflowVisible(context))
            return "visible";

        return "clip";
    }

    public boolean getValueShrinkHorz(FormInstanceContext context) {
        if(valueShrinkHorz != null)
            return valueShrinkHorz;

        if (isProperty(context)) {
            Type type = getAssertValueType(context);
            if (type != null)
                return type.getValueShrinkHorz();
        }

        return false;
    }

    public boolean getValueShrinkVert(FormInstanceContext context) {
        if(valueShrinkVert != null)
            return valueShrinkVert;

        if (isProperty(context)) {
            Type type = getAssertValueType(context);
            if (type != null)
                return type.getValueShrinkVert();
        }

        return false;
    }

    public String getInputType(FormInstanceContext context) {
        if(inputType != null)
            return inputType;

        if(isProperty(context)) {
            Type type = getAssertCellType(context);
            if(type != null)
                return type.getInputType(context);
        }

        return null;
    }

    public String getTag(FormInstanceContext context) {
        if(tag != null)
            return tag.isEmpty() ? null : tag;

        if(isCustom(context)) {
            PropertyDrawEntity.Select select = entity.getSelectProperty(context);
            if (select != null && select.elementType.equals("Dropdown") && Settings.get().isNoToolbarForSelectDropdownInPanel())
                return "select";
            return null;
        }

        Type changeType = getChangeType(context, false);
        if (isProperty(context)) {
            Type type = getAssertCellType(context);
            if(type instanceof LinkClass)
                return "a";
            if((type != null && type.useInputTag(!entity.isList(context), context.useBootstrap, changeType)))
                return "input";

            if(isLink(context) && !hasFlow(context, ChangeFlowType.INPUT))
                return "a";
        } else {
            if(changeType == null && hasFlow(context, ChangeFlowType.ANYEFFECT))
                return "button";
        }

        return null;
    }

    private boolean hasBackground() {
        return design.background != null || entity.getPropertyExtra(BACKGROUND) != null;
    }

    private boolean isLink(FormInstanceContext context) {
        return hasFlow(context, ChangeFlowType.INTERACTIVEFORM) && !hasFlow(context, ChangeFlowType.READONLYCHANGE);
    }

    public String getCaptionElementClass(FormInstanceContext context) {
        if (captionElementClass != null)
            return captionElementClass;

        if(isProperty(context)) {
            String valueElementClass = getValueElementClass(context);
            // shortcut for the toggle button checkbox
            if(valueElementClass != null && valueElementClass.contains("btn-check"))
                return "btn btn-outline-primary";

            if (valueElementClass == null && isSimplePanelText(context))
                return "text-secondary";
        }

        return null;
    }

    @Override
    protected String getDefaultElementClass(FormInstanceContext context) {
        if(isProperty(context)) {
            Type type = getAssertCellType(context);
            if (type instanceof LogicalClass && entity.isPredefinedSwitch())
                return "form-switch";
        }

        return null;
    }

    public String getValueElementClass(FormInstanceContext context) {
        if(valueElementClass != null)
            return valueElementClass;

        if (isProperty(context)) {
            if(isTagInput(context)) {
                Type type = getAssertCellType(context);
                if(type instanceof LogicalClass)
                    return "form-check-input";

                String inputType = getInputType(context);
                if(inputType.equals("range"))
                    return "form-range";

                if(!entity.isList(context)) {
                    if(type instanceof ColorClass)
                        return "form-control form-control-color";

                    return "form-control";
                }
            } else if (isSimplePanelText(context)) {
                // if we're in panel and there is no decoration, nor other styling, nor custom view, making label gray to distinguish it from the value
                if (!context.useBootstrap)
                    return "form-control";

                if (hasFlow(context, ChangeFlowType.INPUT) || hasBackground())
                    return "form-control";

                if (hasChangeAction(context))
                    return "btn btn-light";
            }
        } else {
            if(hasFlow(context, ChangeFlowType.PRIMARY))
                return "btn-primary";

            if(isLink(context))
                return "btn-link text-decoration-none";

            if(hasFlow(context, ChangeFlowType.ANYEFFECT))
                return "btn-outline-secondary";
        }

        return null;
    }

    public String getCommentElementClass(FormInstanceContext context) {
        if(commentElementClass != null)
            return commentElementClass;

        return "form-text";
    }

    private boolean isSimplePanelText(FormInstanceContext context) {
        return getTag(context) == null && !entity.isList(context) && !isCustom(context);
    }

    public boolean hasToolbar(FormInstanceContext context) {
        if(toolbar != null)
            return toolbar;

        if(isCustom(context) && entity.getSelectProperty(context) == null) // we want loading for select props, (entity.isReadOnly(context) || !hasChangeAction(context)) the problem of using hasChangeAction is that for JSON property it is always generated but it's impossible to understand if it is used
            return false;

        if(!isProperty(context))
            return true;

        Type type = getAssertCellType(context);
        if(type != null)
            return type.hasToolbar(!entity.isList(context) && isTagInput(context));

        return true;
    }

    public boolean hasToolbarActions(FormInstanceContext context) {
        if(toolbarActions != null)
            return toolbarActions;

        if (!isProperty(context) || entity.getSelectProperty(context) != null)
            return false;

        return true;
    }

    private boolean isTagInput(FormInstanceContext context) {
        String tag = getTag(context);
        return tag != null && tag.equals("input");
    }

    public Boolean boxed;

    protected boolean isCustom(FormInstanceContext context) {
        return getCustomRenderFunction(context) != null;
    }
}