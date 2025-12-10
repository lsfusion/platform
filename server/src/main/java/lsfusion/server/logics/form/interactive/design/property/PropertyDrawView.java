package lsfusion.server.logics.form.interactive.design.property;

import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.classes.DataType;
import lsfusion.interop.form.event.InputBindingEvent;
import lsfusion.interop.form.print.ReportFieldExtraType;
import lsfusion.interop.form.property.Compare;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.base.AppServerImage;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFProperty;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.TypeSerializer;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.*;
import lsfusion.server.logics.classes.data.file.FileClass;
import lsfusion.server.logics.classes.data.file.RenderedClass;
import lsfusion.server.logics.classes.data.integral.IntegerClass;
import lsfusion.server.logics.classes.data.link.LinkClass;
import lsfusion.server.logics.classes.data.time.IntervalClass;
import lsfusion.server.logics.classes.data.time.TimeSeriesClass;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.action.async.AsyncEventExec;
import lsfusion.server.logics.form.interactive.action.async.AsyncInput;
import lsfusion.server.logics.form.interactive.action.async.AsyncNoWaitExec;
import lsfusion.server.logics.form.interactive.action.async.AsyncSerializer;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.FormInstanceContext;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.BaseComponentView;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.filter.FilterView;
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

public class PropertyDrawView extends BaseComponentView implements PropertyDrawViewOrPivotColumn {

    public PropertyDrawEntity<?> entity;

    private NFProperty<Boolean> changeOnSingleClick = NFFact.property();
    private NFProperty<Long> maxValue = NFFact.property();
    private NFProperty<Boolean> echoSymbols = NFFact.property();
    private NFProperty<Boolean> noSort = NFFact.property();
    private NFProperty<Compare> defaultCompare = NFFact.property();

    private NFProperty<Integer> charWidth = NFFact.property();
    private NFProperty<Integer> charHeight = NFFact.property();

    private NFProperty<Integer> valueWidth = NFFact.property();
    private NFProperty<Integer> valueHeight = NFFact.property();

    private NFProperty<Integer> captionWidth = NFFact.property();
    private NFProperty<Integer> captionHeight = NFFact.property();
    private NFProperty<Integer> captionCharHeight = NFFact.property();

    private NFProperty<Boolean> valueFlex = NFFact.property();

    private NFProperty<String> tag = NFFact.property();
    private NFProperty<String> inputType = NFFact.property();
    private NFProperty<String> valueElementClass = NFFact.property();
    private NFProperty<String> captionElementClass = NFFact.property();

    private NFProperty<Boolean> panelCustom = NFFact.property();

    private NFProperty<InputBindingEvent> changeKey = NFFact.property();
    private NFProperty<Boolean> showChangeKey = NFFact.property();
    private NFProperty<InputBindingEvent> changeMouse = NFFact.property();
    private NFProperty<Boolean> showChangeMouse = NFFact.property();

    private NFProperty<Boolean> drawAsync = NFFact.property();

    private NFProperty<Boolean> inline = NFFact.property();

    private NFProperty<Boolean> focusable = NFFact.property();

    private NFProperty<Boolean> panelColumnVertical = NFFact.property();

    private NFProperty<FlexAlignment> valueAlignmentHorz = NFFact.property();
    private NFProperty<FlexAlignment> valueAlignmentVert = NFFact.property();

    private NFProperty<String> valueOverflowHorz = NFFact.property();
    private NFProperty<String> valueOverflowVert = NFFact.property();

    private NFProperty<Boolean> valueShrinkHorz = NFFact.property();
    private NFProperty<Boolean> valueShrinkVert = NFFact.property();

    private NFProperty<LocalizedString> comment = NFFact.property();
    private NFProperty<String> commentElementClass = NFFact.property();
    private NFProperty<Boolean> panelCommentVertical = NFFact.property();
    private NFProperty<Boolean> panelCommentFirst = NFFact.property();
    private NFProperty<FlexAlignment> panelCommentAlignment = NFFact.property();

    private NFProperty<LocalizedString> placeholder = NFFact.property();
    private NFProperty<LocalizedString> pattern = NFFact.property();
    private NFProperty<LocalizedString> regexp = NFFact.property();
    private NFProperty<LocalizedString> regexpMessage = NFFact.property();

    private NFProperty<LocalizedString> tooltip = NFFact.property();
    private NFProperty<LocalizedString> valueTooltip = NFFact.property();

    private NFProperty<LocalizedString> caption = NFFact.property();
    private NFProperty<AppServerImage.Reader> image = NFFact.property();

    private NFProperty<Boolean> wrap = NFFact.property();
    private NFProperty<Boolean> wrapWordBreak = NFFact.property();
    private NFProperty<Boolean> collapse = NFFact.property();
    private NFProperty<Boolean> ellipsis = NFFact.property();

    private NFProperty<Boolean> captionWrap = NFFact.property();
    private NFProperty<Boolean> captionWrapWordBreak = NFFact.property();
    private NFProperty<Boolean> captionCollapse = NFFact.property();
    private NFProperty<Boolean> captionEllipsis = NFFact.property();

    private NFProperty<Boolean> clearText = NFFact.property();
    private NFProperty<Boolean> notSelectAll = NFFact.property();

    private NFProperty<Boolean> toolbar = NFFact.property();
    private NFProperty<Boolean> toolbarActions = NFFact.property();

    private final NFProperty<Boolean> notNull = NFFact.property();

    private final NFProperty<Boolean> sticky = NFFact.property();
    private final NFProperty<Boolean> sync = NFFact.property();

    private final NFProperty<Boolean> highlightDuplicate = NFFact.property();

    @SuppressWarnings({"UnusedDeclaration"})
    public PropertyDrawView() {

    }

    public PropertyDrawView(PropertyDrawEntity entity, Version version) {
        super(entity.ID);

        this.entity = entity;
        this.entity.view = this;

        setMargin(2, version);
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
        // the main problem here is that in the boolean default caption last we don't know if it's gonna be aligned, so we'll use that hack in PropertyPanelRenderer.initCaption for now (until we'll move isAlignCaptions to the serversion)
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

    public AppServerImage.AutoName getAutoName() {
        return AppServerImage.getAutoName(this::getCaption, entity.getInheritedProperty()::getName);
    }

    public void setImage(String image, Version version) {
        this.setImage(AppServerImage.createPropertyImage(image, this), version);
    }

    private AppServerImage getDefaultImage(ConnectionContext context) {
        return ActionOrProperty.getDefaultImage(AppServerImage.AUTO, getAutoName(), Settings.get().getDefaultPropertyImageRankingThreshold(), Settings.get().isDefaultPropertyImage(), context);
    }

    // we return to the client null, if we're sure that caption is always empty (so we don't need to draw label)
    public String getDrawCaption() {
        LocalizedString caption = getCaption();
        if(hasNoCaption(caption, entity.getPropertyExtra(CAPTION), getElementClass()))
            return null;

        return ThreadLocalContext.localize(caption);
    }

    public static boolean hasNoCaption(LocalizedString caption, PropertyObjectEntity<?> propertyCaption, String elementClass) {
        return ((caption == null || (caption.isEmpty() && elementClass == null)) && propertyCaption == null) || (propertyCaption != null && propertyCaption.property.isExplicitNull()); // isEmpty can be better, but we just want to emulate NULL to be like NULL caption
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

        String reportPattern = getPatternString();
        if(reportPattern != null)
            reportField.pattern = reportPattern;

        return reportField;
    }

    private void setupGeometry(ReportDrawField reportField, int scale) {
        reportField.scale = scale;

        if(entity.isStaticProperty()) {
            Type type = entity.getStaticType();
            if(type != null) {
                reportField.minimumWidth = type.getReportMinimumWidth() * scale;
                reportField.preferredWidth = type.getReportPreferredWidth() * scale;
            }
        }
        Integer reportCharWidth = getCharWidth();
        if (reportCharWidth != null) {
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
        ReportDrawField field = new ReportDrawField("", "", 0);
        Type type = property.property.getType();
        if (type != null) {
            type.fillReportDrawField(field);
        }
        return field.valueClass;
    }

    private static boolean containsClass(String aClass, String check) {
        return aClass != null && aClass.contains(check);
    }

    private OrderedMap<String, ContextMenuInfo> filterContextMenuItems(OrderedMap<String, ActionOrProperty.ContextMenuBinding> contextMenuBindings, FormInstanceContext context) {
        if (contextMenuBindings == null || contextMenuBindings.isEmpty()) {
            return null;
        }

        OrderedMap<String, ContextMenuInfo> contextMenuItems = new OrderedMap<>();
        for (int i = 0; i < contextMenuBindings.size(); ++i) {
            String actionSID = contextMenuBindings.getKey(i);
            ActionOrProperty.ContextMenuBinding binding = contextMenuBindings.getValue(i);
            if(binding.show(this, context)) {
                ActionObjectEntity<?> eventAction = entity.getCheckedEventAction(actionSID, context);
                if (eventAction != null && context.securityPolicy.checkPropertyViewPermission(eventAction.property)) {
                    contextMenuItems.put(actionSID, binding.action != null ?
                            new ContextMenuInfo(binding.caption, binding.action.getActionOrProperty().getSID(), binding.action.getCreationPath(), binding.action.getPath())
                            : new ContextMenuInfo(binding.caption, actionSID, eventAction.getCreationPath(), eventAction.getPath()));
                }
            }
        }
        return contextMenuItems;
    }

    private static class ContextMenuInfo {
        private LocalizedString caption;
        private String sid;
        private String creationPath;
        private String path;

        public ContextMenuInfo(LocalizedString caption, String sid, String creationPath, String path) {
            this.caption = caption;
            this.sid = sid;
            this.creationPath = creationPath;
            this.path = path;
        }
    }

    @Override
    public String toString() {
        return ThreadLocalContext.localize(getCaption()) + " " + super.toString();
    }

    // the same is on the client
    private static int getScaledCharWidth(long count, long charWidth) {
        return (int) round((count <= 3.0 ? Math.max(count, 1.0) : 3.0 + pow(count - 3.0, 0.5)) * charWidth);
    }

    public boolean isHorizontalValueFlex(FormInstanceContext context) {
        Boolean valueFlex = getValueFlex();
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
    public boolean hasUserChangeAction(FormInstanceContext context) {
        if(!hasChangeAction(context))
            return false;

        if (isProperty(context) && getAssertCellType(context) instanceof HTMLTextClass)
            return getExternalChangeType(context) instanceof HTMLTextClass;

        // if custom render change is the input of some type, then probably it is a programmatic change (i.e. custom renderer uses changeValue to set this value, and should not be replaced with the input)
        return getCustomRenderFunction(context) == null || getExternalChangeType(context) == null;
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

    private boolean hasBackground() {
        return getBackground() != null || entity.getPropertyExtra(BACKGROUND) != null;
    }

    private boolean isLink(FormInstanceContext context) {
        return hasFlow(context, ChangeFlowType.INTERACTIVEFORM) && !hasFlow(context, ChangeFlowType.READONLYCHANGE);
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        super.customSerialize(pool, outStream);

        pool.writeString(outStream, getDrawCaption());
        AppServerImage.serialize(getImage(pool.context), outStream, pool);
        pool.writeLong(outStream, getMaxValue());
        outStream.writeBoolean(getEchoSymbols());
        outStream.writeBoolean(isNoSort());

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
        outStream.writeInt(getCaptionCharHeight(pool.context));

        pool.writeObject(outStream, getChangeKey());
        outStream.writeBoolean(getShowChangeKey());
        pool.writeObject(outStream, getChangeMouse());
        outStream.writeBoolean(getShowChangeMouse());

        outStream.writeBoolean(isDrawAsync());

        pool.writeObject(outStream, getInline());
        outStream.writeBoolean(entity.isList(pool.context));

        pool.writeObject(outStream, getFocusable());
        outStream.writeByte(entity.getEditType().serialize());

        outStream.writeBoolean(isPanelCustom(pool.context));
        outStream.writeBoolean(isPanelColumnVertical());

        pool.writeObject(outStream, getValueAlignmentHorz(pool.context));
        pool.writeObject(outStream, getValueAlignmentVert(pool.context));

        pool.writeBoolean(outStream, highlightDuplicateValue(pool.context));

        pool.writeString(outStream, getValueOverflowHorz(pool.context));
        pool.writeString(outStream, getValueOverflowVert(pool.context));

        pool.writeBoolean(outStream, getValueShrinkHorz(pool.context));
        pool.writeBoolean(outStream, getValueShrinkVert(pool.context));

        pool.writeString(outStream, ThreadLocalContext.localize(getComment()));
        pool.writeString(outStream, getCommentElementClass(pool.context));
        outStream.writeBoolean(isPanelCommentVertical());
        outStream.writeBoolean(isPanelCommentFirst(pool.context));
        pool.writeObject(outStream, getPanelCommentAlignment(pool.context));

        pool.writeString(outStream, ThreadLocalContext.localize(getPlaceholder(pool.context)));
        pool.writeString(outStream, ThreadLocalContext.localize(getPattern(pool.context)));
        pool.writeString(outStream, ThreadLocalContext.localize(getRegexp()));
        pool.writeString(outStream, ThreadLocalContext.localize(getRegexpMessage()));

        pool.writeString(outStream, ThreadLocalContext.localize(getTooltip()));
        pool.writeString(outStream, ThreadLocalContext.localize(getValueTooltip()));

        pool.writeObject(outStream, getChangeOnSingleClick(pool.context));
        outStream.writeBoolean(entity.hide);
        outStream.writeBoolean(entity.remove);

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
        outStream.writeBoolean(hasUserChangeAction(pool.context));
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

        outStream.writeBoolean(isWrap(pool.context));
        outStream.writeBoolean(isWrapWordBreak());
        outStream.writeBoolean(isCollapse(pool.context));
        outStream.writeBoolean(isEllipsis(pool.context));

        outStream.writeBoolean(isCaptionWrap(pool.context));
        outStream.writeBoolean(isCaptionWrapWordBreak(pool.context));
        outStream.writeBoolean(isCaptionCollapse(pool.context));
        outStream.writeBoolean(isCaptionEllipsis());

        outStream.writeBoolean(isClearText());
        outStream.writeBoolean(getNotSelectAll());

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

        pool.serializeObject(outStream, entity.quickFilterProperty != null ? pool.context.view.get(entity.quickFilterProperty) : null);

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

        OrderedMap<String, ContextMenuInfo> contextMenuBindings = filterContextMenuItems(entity.getContextMenuBindings(pool.context), pool.context);
        outStream.writeInt(contextMenuBindings == null ? 0 : contextMenuBindings.size());
        if (contextMenuBindings != null) {
            for (int i = 0; i < contextMenuBindings.size(); ++i) {
                String actionSID = contextMenuBindings.getKey(i);
                ContextMenuInfo info = contextMenuBindings.getValue(i);
                pool.writeString(outStream, actionSID);
                pool.writeString(outStream, ThreadLocalContext.localize(info.caption));
                boolean hasDebugInfo = info.sid != null;
                pool.writeBoolean(outStream, hasDebugInfo);
                if(hasDebugInfo) {
                    pool.writeString(outStream, info.sid);
                    pool.writeString(outStream, info.creationPath);
                    pool.writeString(outStream, info.path);
                }
            }
        }

        outStream.writeBoolean(isNotNull());
        outStream.writeBoolean(isSticky(pool.context));
        outStream.writeBoolean(entity.hasActiveProperty());
        outStream.writeBoolean(entity.getPropertyExtra(PropertyDrawExtraType.FOOTER) != null);
    }

    public Boolean getChangeOnSingleClick(FormInstanceContext context) {
        Boolean changeOnSingleClick = getChangeOnSingleClick();
        if(changeOnSingleClick != null)
            return changeOnSingleClick;

        if(isProperty(context)) {
            if (getAssertCellType(context) instanceof LogicalClass)
                return Settings.get().getChangeBooleanOnSingleClick();
        } else
            return Settings.get().getChangeActionOnSingleClick();

        return null;
    }
    public Boolean getChangeOnSingleClick() {
        return changeOnSingleClick.get();
    }
    public void setChangeOnSingleClick(Boolean value, Version version) {
        changeOnSingleClick.set(value, version);
    }

    public Long getMaxValue() {
        return maxValue.get();
    }
    public void setMaxValue(Long value, Version version) {
        maxValue.set(value, version);
    }

    public Boolean getEchoSymbols() {
        return echoSymbols.get();
    }
    public Boolean getEchoSymbolsNF(Version version) {
        return echoSymbols.getNF(version);
    }
    public void setEchoSymbols(Boolean value, Version version) {
        echoSymbols.set(value, version);
    }

    public boolean isNoSort() {
        return nvl(noSort.get(), false);
    }
    public void setNoSort(Boolean value, Version version) {
        noSort.set(value, version);
    }

    public Compare getDefaultCompare(FormInstanceContext context) {
        Compare defaultCompare = getDefaultCompare();
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
    public Compare getDefaultCompare() {
        return defaultCompare.get();
    }
    public Compare getDefaultCompareNF(Version version) {
        return defaultCompare.getNF(version);
    }
    public void setDefaultCompare(Compare value, Version version) {
        defaultCompare.set(value, version);
    }

    public int getCharWidth(FormInstanceContext context) {
        Integer charWidth = getAdjustedCharWidth(context);
        return charWidth != null ? charWidth : -1;
    }

    private Integer getAdjustedCharWidth(FormInstanceContext context) {
        PropertyDrawEntity.Select select = entity.getSelectProperty(context);
        if(select != null) {
            Integer charWidth = this.getCharWidth(); // select.elementType.startsWith("Button") && select.actual ? null :

            long elementCharWidth = charWidth != null ? charWidth : (select.count > 0 ? select.length / select.count : 0);

            if(select.elementType.equals("Input") || (select.elementType.equals("Dropdown") && select.type.equals("Multi")))
                return getScaledCharWidth(4, elementCharWidth);

//            if (!entity.isList(context)) // we ignore charWidth in panel buttons and lists
//                return null;

            if(entity.isList(context) || charWidth != null) {
                if (select.elementType.startsWith("Button"))
                    return getScaledCharWidth(select.count, (elementCharWidth + (select.elementType.startsWith("ButtonGroup") ? 4 : 6))); // couple of symbols for padding

                if (select.elementType.equals("List") || select.elementType.equals("Dropdown"))
                    return (int) (elementCharWidth + 4); // couple of symbols for control elements, && !select.actual
            }

            return null;
        }
        return getCharWidth();
    }
    public Integer getCharWidth() {
        return charWidth.get();
    }
    public Integer getCharWidthNF(Version version) {
        return charWidth.getNF(version);
    }
    public void setCharWidth(Integer value, Version version) {
        charWidth.set(value, version);
    }

    public Integer getCharHeight() {
        return nvl(charHeight.get(), -1);
    }
    public void setCharHeight(Integer value, Version version) {
        charHeight.set(value, version);
    }

    public int getValueWidth(FormInstanceContext context) {
        Integer valueWidth = getValueWidth();
        if(valueWidth != null)
            return valueWidth;

        Type valueType;
        if (getAdjustedCharWidth(context) != null || (!isCustom(context) && isProperty(context) && !((valueType = getAssertValueType(context)) instanceof LogicalClass || valueType instanceof FileClass)))
            return -2;

        return -1;
    }
    public Integer getValueWidth() {
        return valueWidth.get();
    }
    public Integer getValueWidthNF(Version version) {
        return valueWidth.getNF(version);
    }
    public void setValueWidth(Integer value, Version version) {
        valueWidth.set(value, version);
    }

    public int getValueHeight(FormInstanceContext context) {
        Integer valueHeight = getValueHeight();
        if(valueHeight != null)
            return valueHeight;

        if (getCharHeight() != null)
            return -2;

        if (!isCustom(context) && isProperty(context)) {
            Type valueType = getAssertValueType(context);
            if (valueType instanceof TextClass || (valueType instanceof RenderedClass && entity.isList(context))) { // in grid rendered classes still have small fixed width, so the height also better to be small
                return -2;
            }
        }

        return -1;
    }
    public Integer getValueHeight() {
        return valueHeight.get();
    }
    public Integer getValueHeightNF(Version version) {
        return valueHeight.getNF(version);
    }
    public void setValueHeight(Integer value, Version version) {
        valueHeight.set(value, version);
    }

    public int getCaptionWidth(FormEntity entity) {
        Integer captionWidth = getCaptionWidth();
        if(captionWidth != null)
            return captionWidth;

        return -1;
    }
    public Integer getCaptionWidth() {
        return captionWidth.get();
    }
    public Integer getCaptionWidthNF(Version version) {
        return captionWidth.getNF(version);
    }
    public void setCaptionWidth(Integer value, Version version) {
        captionWidth.set(value, version);
    }

    public int getCaptionHeight(FormInstanceContext context) {
        Integer captionHeight = getCaptionHeight();
        if(captionHeight != null)
            return captionHeight;

        Integer captionCharHeight = getCaptionCharHeight();
        if(captionCharHeight != null || (moreInfo && entity.isList(context)))
            return -2;

        return -1;
    }
    public Integer getCaptionHeight() {
        return captionHeight.get();
    }
    public Integer getCaptionHeightNF(Version version) {
        return captionHeight.getNF(version);
    }
    public void setCaptionHeight(Integer value, Version version) {
        captionHeight.set(value, version);
    }

    public int getCaptionCharHeight(FormInstanceContext context) {
        Integer captionCharHeight = getCaptionCharHeight();
        if(captionCharHeight != null)
            return captionCharHeight;

        return 3;
    }
    public Integer getCaptionCharHeight() {
        return captionCharHeight.get();
    }
    public void setCaptionCharHeight(Integer value, Version version) {
        captionCharHeight.set(value, version);
    }

    public Boolean getValueFlex() {
        return valueFlex.get();
    }
    public Boolean getValueFlexNF(Version version) {
        return valueFlex.getNF(version);
    }
    public void setValueFlex(Boolean value, Version version) {
        valueFlex.set(value, version);
    }

    public String getTag(FormInstanceContext context) {
        String tag = getTag();
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
    public String getTag() {
        return tag.get();
    }
    public void setTag(String value, Version version) {
        tag.set(value, version);
    }

    public String getInputType(FormInstanceContext context) {
        String inputType = getInputType();
        if(inputType != null)
            return inputType;

        Boolean echoSymbols = getEchoSymbols();
        if(echoSymbols != null && echoSymbols)
            return "password";

        if(isProperty(context)) {
            Type type = getAssertCellType(context);
            if(type != null)
                return type.getInputType(context);
        }

        return null;
    }
    public String getInputType() {
        return inputType.get();
    }
    public void setInputType(String value, Version version) {
        inputType.set(value, version);
    }

    public String getValueElementClass(FormInstanceContext context) {
        String valueElementClass = getValueElementClass();
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
    public String getValueElementClass() {
        return valueElementClass.get();
    }
    public void setValueElementClass(String value, Version version) {
        valueElementClass.set(value, version);
    }

    public String getCaptionElementClass(FormInstanceContext context) {
        String captionElementClass = getCaptionElementClass();
        if (captionElementClass != null)
            return captionElementClass;

        if(isProperty(context)) {
            String valueElementClass = getValueElementClass(context);
            // shortcut for the toggle button checkbox
            if(containsClass(valueElementClass, "btn-check"))
                return "btn btn-outline-primary";

            if (valueElementClass == null && isSimplePanelText(context))
                return "text-secondary";
        }

        return null;
    }
    public String getCaptionElementClass() {
        return captionElementClass.get();
    }

    public void setCaptionElementClass(String value, Version version) {
        captionElementClass.set(value, version);
    }

    public boolean isPanelCustom(FormInstanceContext context) {
        Boolean panelCustom = getPanelCustom();
        if(panelCustom != null)
            return panelCustom;

        // form-check needs its own layouting
        return containsClass(getElementClass(context), "form-check");
    }
    public Boolean getPanelCustom() {
        return panelCustom.get();
    }
    public void setPanelCustom(Boolean value, Version version) {
        panelCustom.set(value, version);
    }

    public InputBindingEvent getChangeKey() {
        return changeKey.get();
    }
    public InputBindingEvent getChangeKeyNF(Version version) {
        return changeKey.getNF(version);
    }
    public void setChangeKey(InputBindingEvent value, Version version) {
        changeKey.set(value, version);
    }

    public Boolean getShowChangeKey() {
        return showChangeKey.get();
    }
    public Boolean getShowChangeKeyNF(Version version) {
        return showChangeKey.getNF(version);
    }
    public void setShowChangeKey(Boolean value, Version version) {
        showChangeKey.set(value, version);
    }

    public InputBindingEvent getChangeMouse() {
        return changeMouse.get();
    }
    public InputBindingEvent getChangeMouseNF(Version version) {
        return changeMouse.getNF(version);
    }
    public void setChangeMouse(InputBindingEvent value, Version version) {
        changeMouse.set(value, version);
    }

    public Boolean getShowChangeMouse() {
        return showChangeMouse.get();
    }
    public Boolean getShowChangeMouseNF(Version version) {
        return showChangeMouse.getNF(version);
    }
    public void setShowChangeMouse(Boolean value, Version version) {
        showChangeMouse.set(value, version);
    }

    public boolean isDrawAsync() {
        return nvl(drawAsync.get(), false);
    }
    public void setDrawAsync(Boolean value, Version version) {
        drawAsync.set(value, version);
    }

    public Boolean getInline() {
        return inline.get();
    }
    public void setInline(Boolean value, Version version) {
        inline.set(value, version);
    }

    public Boolean getFocusable() {
        return focusable.get();
    }
    public void setFocusable(Boolean value, Version version) {
        focusable.set(value, version);
    }

    public Boolean isPanelColumnVertical() {
        return nvl(panelColumnVertical.get(), false);
    }
    public void setPanelColumnVertical(Boolean value, Version version) {
        panelColumnVertical.set(value, version);
    }

    public FlexAlignment getValueAlignmentHorz(FormInstanceContext context) {
        FlexAlignment valueAlignmentHorz = getValueAlignmentHorz();
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
    public FlexAlignment getValueAlignmentHorz() {
        return valueAlignmentHorz.get();
    }
    public void setValueAlignmentHorz(FlexAlignment value, Version version) {
        valueAlignmentHorz.set(value, version);
    }

    public FlexAlignment getValueAlignmentVert(FormInstanceContext context) {
        FlexAlignment valueAlignmentVert = getValueAlignmentVert();
        if (valueAlignmentVert != null)
            return valueAlignmentVert;

        if(isProperty(context)) {
            Type type = getAssertValueType(context);
            if (type != null)
                return type.getValueAlignmentVert();
        }

        return FlexAlignment.CENTER;
    }
    public FlexAlignment getValueAlignmentVert() {
        return valueAlignmentVert.get();
    }
    public void setValueAlignmentVert(FlexAlignment value, Version version) {
        valueAlignmentVert.set(value, version);
    }

    public String getValueOverflowHorz(FormInstanceContext context) {
        String valueOverflowHorz = getValueOverflowHorz();
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
    public String getValueOverflowHorz() {
        return valueOverflowHorz.get();
    }
    public void setValueOverflowHorz(String value, Version version) {
        valueOverflowHorz.set(value, version);
    }

    public String getValueOverflowVert(FormInstanceContext context) {
        String valueOverflowVert = getValueOverflowVert();
        if(valueOverflowVert != null)
            return valueOverflowVert;

        if(isShrinkOverflowVisible(context))
            return "visible";

        return "clip";
    }
    public String getValueOverflowVert() {
        return valueOverflowVert.get();
    }
    public void setValueOverflowVert(String value, Version version) {
        valueOverflowVert.set(value, version);
    }

    public boolean getValueShrinkHorz(FormInstanceContext context) {
        Boolean valueShrinkHorz = getValueShrinkHorz();
        if(valueShrinkHorz != null)
            return valueShrinkHorz;

        if (isProperty(context)) {
            Type type = getAssertValueType(context);
            if (type != null)
                return type.getValueShrinkHorz();
        }

        return false;
    }
    public Boolean getValueShrinkHorz() {
        return valueShrinkHorz.get();
    }
    public void setValueShrinkHorz(Boolean value, Version version) {
        valueShrinkHorz.set(value, version);
    }

    public boolean getValueShrinkVert(FormInstanceContext context) {
        Boolean valueShrinkVert = getValueShrinkVert();
        if(valueShrinkVert != null)
            return valueShrinkVert;

        if (isProperty(context)) {
            Type type = getAssertValueType(context);
            if (type != null)
                return type.getValueShrinkVert();
        }

        return false;
    }
    public Boolean getValueShrinkVert() {
        return valueShrinkVert.get();
    }
    public void setValueShrinkVert(Boolean value, Version version) {
        valueShrinkVert.set(value, version);
    }

    public LocalizedString getComment() {
        return comment.get();
    }
    public LocalizedString getCommentNF(Version version) {
        return comment.getNF(version);
    }
    public void setComment(LocalizedString value, Version version) {
        comment.set(value, version);
    }

    public String getCommentElementClass(FormInstanceContext context) {
        String commentElementClass = getCommentElementClass();
        if(commentElementClass != null)
            return commentElementClass;

        return "form-text";
    }
    public String getCommentElementClass() {
        return commentElementClass.get();
    }
    public void setCommentElementClass(String value, Version version) {
        commentElementClass.set(value, version);
    }

    public boolean isPanelCommentVertical() {
        return nvl(panelCommentVertical.get(), false);
    }
    public void setPanelCommentVertical(Boolean value, Version version) {
        panelCommentVertical.set(value,version);
    }

    protected boolean isPanelCommentFirst(FormInstanceContext context) {
        Boolean panelCommentFirst = getPanelCommentFirst();
        if(panelCommentFirst != null)
            return panelCommentFirst;

        return false;
    }
    public Boolean getPanelCommentFirst() {
        return panelCommentFirst.get();
    }
    public void setPanelCommentFirst(Boolean value, Version version) {
        panelCommentFirst.set(value,version);
    }

    protected FlexAlignment getPanelCommentAlignment(FormInstanceContext context) {
        FlexAlignment panelCommentAlignment = getPanelCommentAlignment();
        if(panelCommentAlignment != null)
            return panelCommentAlignment;

        return FlexAlignment.CENTER;
    }
    public FlexAlignment getPanelCommentAlignment() {
        return panelCommentAlignment.get();
    }
    public void setPanelCommentAlignment(FlexAlignment value,Version version) {
        panelCommentAlignment.set(value,version);
    }

    private LocalizedString getPlaceholder(FormInstanceContext context) {
        LocalizedString placeholder = getPlaceholder();
        if(placeholder != null)
            return placeholder;

        if (isProperty(context)) {
            String tag = getTag(context);
            if (tag != null && tag.equals("a") && getAssertValueType(context) instanceof StringClass)
                return LocalizedString.create("{form.renderer.not.defined}");
        }

        return null;
    }
    public LocalizedString getPlaceholder() {
        return placeholder.get();
    }
    public void setPlaceholder(LocalizedString value, Version version) {
        placeholder.set(value,version);
    }

    private LocalizedString getPattern(FormInstanceContext context) {
        LocalizedString pattern = getPattern();
        if(pattern != null)
            return pattern;

        if (isProperty(context)) {
            String inputType = getInputType(context);
            if (inputType != null && inputType.equals("year"))
                return LocalizedString.create("{####}");
        }

        return null;
    }
    public String getPatternString() {
        LocalizedString p = getPattern();
        return p != null ? p.getSourceString() : null;
    }
    public LocalizedString getPattern() {
        return pattern.get();
    }
    public LocalizedString getPatternNF(Version version) {
        return pattern.getNF(version);
    }
    public void setPattern(LocalizedString value, Version version) {
        pattern.set(value,version);
    }

    public LocalizedString getRegexp() {
        return regexp.get();
    }
    public LocalizedString getRegexpNF(Version version) {
        return regexp.getNF(version);
    }

    public void setRegexp(LocalizedString value, Version version) {
        regexp.set(value,version);
    }

    public LocalizedString getRegexpMessage() {
        return regexpMessage.get();
    }

    public LocalizedString getRegexpMessageNF(Version version) {
        return regexpMessage.getNF(version);
    }

    public void setRegexpMessage(LocalizedString value, Version version) {
        regexpMessage.set(value,version);
    }


    public LocalizedString getTooltip() {
        return tooltip.get();
    }
    public LocalizedString getTooltipNF(Version version) {
        return tooltip.getNF(version);
    }
    public void setTooltip(LocalizedString value, Version version) {
        tooltip.set(value,version);
    }

    public LocalizedString getValueTooltip() {
        return valueTooltip.get();
    }
    public LocalizedString getValueTooltipNF(Version version) {
        return valueTooltip.getNF(version);
    }
    public void setValueTooltip(LocalizedString value, Version version) {
        valueTooltip.set(value,version);
    }

    public LocalizedString getCaption() {
        LocalizedString captionValue = caption.get();
        if (captionValue != null)
            return captionValue;

        return entity.getCaption();
    }
    public void setCaption(LocalizedString value, Version version) {
        caption.set(value,version);
    }

    public AppServerImage getImage(ConnectionContext context) {
        AppServerImage.Reader img = image.get();
        if(img != null)
            return img.get(context);

        AppServerImage.Reader entityImage = entity.getImage();
        if(entityImage != null)
            return entityImage.get(context);

        return getDefaultImage(context);
    }
    public void setImage(AppServerImage.Reader value,Version version) {
        image.set(value,version);
    }

    public boolean isWrap(FormInstanceContext context) {
        Boolean wrapValue = wrap.get();
        if (wrapValue != null)
            return wrapValue;

        if (isProperty(context)) {
            Type type = getAssertCellType(context);
            if (type instanceof TextClass)
                return true;

            return context.contentWordWrap;
        }

        return false;
    }
    public void setWrap(Boolean value, Version version) {
        wrap.set(value,version);
    }

    public boolean isWrapWordBreak() {
        Boolean wrap = wrapWordBreak.get();
        if (wrap != null)
            return wrap;

        return moreInfo;
    }
    public void setWrapWordBreak(Boolean value, Version version) {
        wrapWordBreak.set(value,version);
    }

    public boolean isCollapse(FormInstanceContext context) {
        Boolean collapseValue = collapse.get();
        if (collapseValue != null)
            return collapseValue;

        if (isProperty(context)) {
            Type type = getAssertCellType(context);
            if (type instanceof TextClass)
                return false;
        }

        return true;
    }
    public void setCollapse(Boolean value, Version version) {
        collapse.set(value,version);
    }

    private static final ExtInt ELLIPSIS_LIMIT = new ExtInt(40);
    public boolean isEllipsis(FormInstanceContext context) {
        Boolean ellipsisValue = ellipsis.get();
        if (ellipsisValue != null)
            return ellipsisValue;

        if (!moreInfo && isProperty(context)) {
            Type type = getAssertCellType(context);
            if (type instanceof StringClass && ELLIPSIS_LIMIT.less(type.getCharLength()))
                return true;
        }

        return false;
    }
    public void setEllipsis(Boolean value, Version version) {
        ellipsis.set(value,version);
    }

    public boolean isCaptionWrap(FormInstanceContext context) {
        Boolean captionWrapValue = captionWrap.get();
        if (captionWrapValue != null)
            return captionWrapValue;

        return entity.isList(context) || isCaptionVertical(context);
    }
    public void setCaptionWrap(Boolean value, Version version) {
        captionWrap.set(value,version);
    }

    public boolean isCaptionWrapWordBreak(FormInstanceContext context) {
        Boolean captionWrapWordBreakValue = captionWrapWordBreak.get();
        if (captionWrapWordBreakValue != null)
            return captionWrapWordBreakValue;

        return false; // moreInfo; false looks odd with wrap 2, 3 // && entity.isList(context);
    }
    public void setCaptionWrapWordBreak(Boolean value, Version version) {
        captionWrapWordBreak.set(value,version);
    }

    public boolean isCaptionCollapse(FormInstanceContext context) {
        Boolean captionCollapseValue = captionCollapse.get();
        if (captionCollapseValue != null)
            return captionCollapseValue;

        return false;
    }
    public void setCaptionCollapse(Boolean value, Version version) {
        captionCollapse.set(value,version);
    }

    public boolean isCaptionEllipsis() {
        Boolean captionEllipsisValue = captionEllipsis.get();
        if (captionEllipsisValue != null)
            return captionEllipsisValue;

        return !moreInfo;
    }
    public void setCaptionEllipsis(Boolean value, Version version) {
        captionEllipsis.set(value,version);
    }


    public boolean isClearText() {
        return nvl(clearText.get(), false);
    }
    public void setClearText(Boolean value, Version version) {
        clearText.set(value,version);
    }

    public boolean getNotSelectAll() {
        return nvl(notSelectAll.get(), false);
    }
    public void setNotSelectAll(Boolean value, Version version) {
        notSelectAll.set(value,version);
    }

    public boolean hasToolbar(FormInstanceContext context) {
        Boolean toolbarValue = toolbar.get();
        if(toolbarValue != null)
            return toolbarValue;

        if(isCustom(context) && entity.getSelectProperty(context) == null) // we want loading for select props, (entity.isReadOnly(context) || !hasChangeAction(context)) the problem of using hasChangeAction is that for JSON property it is always generated but it's impossible to understand if it is used
            return false;

        if(!isProperty(context))
            return true;

        Type type = getAssertCellType(context);
        if(type != null)
            return type.hasToolbar(!entity.isList(context) && isTagInput(context));

        return true;
    }
    public void setToolbar(Boolean value, Version version) {
        toolbar.set(value,version);
    }

    public boolean hasToolbarActions(FormInstanceContext context) {
        Boolean toolbarActionsValue = toolbarActions.get();
        if(toolbarActionsValue != null)
            return toolbarActionsValue;

        if (!isProperty(context) || entity.getSelectProperty(context) != null)
            return false;

        return true;
    }
    public void setToolbarActions(Boolean value, Version version) {
        toolbarActions.set(value,version);
    }

    public boolean isNotNull() {
        return nvl(notNull.get(), false) || entity.isNotNull();
    }
    public void setNotNull(Boolean value, Version version) {
        notNull.set(value,version);
    }

    public boolean isSticky(FormInstanceContext context) {
        if (entity.sticky != null)
            return entity.sticky;

        Boolean stickyValue = sticky.get();
        if (stickyValue != null)
            return stickyValue;

        if(!isProperty(context))
            return false;

        if(entity.getAssertValueProperty(context).isValueUnique(entity.getToDraw(context.entity), Property.ValueUniqueType.STICKY))
            return true;

        if(((Property<?>)entity.getInheritedProperty()).isId())
            return true;

        return false;
    }
    public Boolean getStickyNF(Version version) {
        return sticky.getNF(version);
    }
    public void setSticky(Boolean value, Version version) {
        sticky.set(value,version);
    }

    public Boolean getSync() {
        return nvl(entity.sync, sync.get());
    }
    public Boolean getSyncNF(Version version) {
        return sync.getNF(version);
    }
    public void setSync(Boolean value, Version version) {
        sync.set(value,version);
    }

    public boolean highlightDuplicateValue(FormInstanceContext context) {
        Boolean highlightDuplicateValue = highlightDuplicate.get();
        if(highlightDuplicateValue != null)
            return highlightDuplicateValue;

        return context.highlightDuplicateValue;
    }
    public void setHighlightDuplicate(Boolean value, Version version) {
        highlightDuplicate.set(value,version);
    }

    @Override
    protected String getDefaultElementClass(FormInstanceContext context) {
        if(isProperty(context)) {
            if (isTagInput(context)) {
                Type type = getAssertCellType(context);
                if(type instanceof LogicalClass) {
                    String logicalClass = null;
                    if(!entity.isList(context))
                        logicalClass = "form-check";
                    if(entity.isPredefinedSwitch())
                        logicalClass = (logicalClass != null ? logicalClass + " " : "") + "form-switch";
                    return logicalClass;
                }
            }
        }

        return null;
    }

    private boolean isSimplePanelText(FormInstanceContext context) {
        return getTag(context) == null && !entity.isList(context) && !isCustom(context);
    }

    private boolean isTagInput(FormInstanceContext context) {
        String tag = getTag(context);
        return tag != null && tag.equals("input");
    }

    protected boolean isCustom(FormInstanceContext context) {
        return getCustomRenderFunction(context) != null;
    }

    public FilterView filter;

    // copy-constructor
    public PropertyDrawView(PropertyDrawView src, ObjectMapping mapping) {
        super(src, mapping);

        entity = mapping.get(src.entity);
        entity.view = this;

        ID = entity.ID;

        changeOnSingleClick.set(src.changeOnSingleClick.get(), mapping.version);
        maxValue.set(src.maxValue.get(), mapping.version);
        echoSymbols.set(src.echoSymbols.get(), mapping.version);
        noSort.set(src.noSort.get(), mapping.version);
        defaultCompare.set(src.defaultCompare.get(), mapping.version);

        charWidth.set(src.charWidth.get(), mapping.version);
        charHeight.set(src.charHeight.get(), mapping.version);

        valueWidth.set(src.valueWidth.get(), mapping.version);
        valueHeight.set(src.valueHeight.get(), mapping.version);

        captionWidth.set(src.captionWidth.get(), mapping.version);
        captionHeight.set(src.captionHeight.get(), mapping.version);
        captionCharHeight.set(src.captionCharHeight.get(), mapping.version);

        valueFlex.set(src.valueFlex.get(), mapping.version);

        tag.set(src.tag.get(), mapping.version);
        inputType.set(src.inputType.get(), mapping.version);
        valueElementClass.set(src.valueElementClass.get(), mapping.version);
        captionElementClass.set(src.captionElementClass.get(), mapping.version);

        panelCustom.set(src.panelCustom.get(), mapping.version);

        changeKey.set(src.changeKey.get(), mapping.version);
        showChangeKey.set(src.showChangeKey.get(), mapping.version);
        changeMouse.set(src.changeMouse.get(), mapping.version);
        showChangeMouse.set(src.showChangeMouse.get(), mapping.version);

        drawAsync.set(src.drawAsync.get(), mapping.version);

        inline.set(src.inline.get(), mapping.version);

        focusable.set(src.focusable.get(), mapping.version);

        panelColumnVertical.set(src.panelColumnVertical.get(), mapping.version);

        valueAlignmentHorz.set(src.valueAlignmentHorz.get(), mapping.version);
        valueAlignmentVert.set(src.valueAlignmentVert.get(), mapping.version);

        valueOverflowHorz.set(src.valueOverflowHorz.get(), mapping.version);
        valueOverflowVert.set(src.valueOverflowVert.get(), mapping.version);

        valueShrinkHorz.set(src.valueShrinkHorz.get(), mapping.version);
        valueShrinkVert.set(src.valueShrinkVert.get(), mapping.version);

        comment.set(src.comment.get(), mapping.version);
        commentElementClass.set(src.commentElementClass.get(), mapping.version);
        panelCommentVertical.set(src.panelCommentVertical.get(), mapping.version);
        panelCommentFirst.set(src.panelCommentFirst.get(), mapping.version);
        panelCommentAlignment.set(src.panelCommentAlignment.get(), mapping.version);

        placeholder.set(src.placeholder.get(), mapping.version);
        pattern.set(src.pattern.get(), mapping.version);
        regexp.set(src.regexp.get(), mapping.version);
        regexpMessage.set(src.regexpMessage.get(), mapping.version);

        tooltip.set(src.tooltip.get(), mapping.version);
        valueTooltip.set(src.valueTooltip.get(), mapping.version);

        caption.set(src.caption.get(), mapping.version);
        image.set(src.image.get(), mapping.version);

        wrap.set(src.wrap.get(), mapping.version);
        wrapWordBreak.set(src.wrapWordBreak.get(), mapping.version);
        collapse.set(src.collapse.get(), mapping.version);
        ellipsis.set(src.ellipsis.get(), mapping.version);

        captionWrap.set(src.captionWrap.get(), mapping.version);
        captionWrapWordBreak.set(src.captionWrapWordBreak.get(), mapping.version);
        captionCollapse.set(src.captionCollapse.get(), mapping.version);
        captionEllipsis.set(src.captionEllipsis.get(), mapping.version);

        clearText.set(src.clearText.get(), mapping.version);
        notSelectAll.set(src.notSelectAll.get(), mapping.version);

        toolbar.set(src.toolbar.get(), mapping.version);
        toolbarActions.set(src.toolbarActions.get(), mapping.version);

        notNull.set(src.notNull.get(), mapping.version);

        sticky.set(src.sticky.get(), mapping.version);
        sync.set(src.sync.get(), mapping.version);

        highlightDuplicate.set(src.highlightDuplicate.get(), mapping.version);
    }
}