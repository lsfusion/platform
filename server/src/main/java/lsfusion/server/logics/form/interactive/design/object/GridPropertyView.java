package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.identity.IDGenerator;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFOrderSet;
import lsfusion.server.base.version.interfaces.NFProperty;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.ServerIdentityObject;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.FormInstanceContext;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.auto.DefaultFormView;
import lsfusion.server.logics.form.interactive.design.filter.FilterControlsView;
import lsfusion.server.logics.form.interactive.design.filter.FilterView;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.interactive.design.property.PropertyGroupContainersView;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.physics.admin.Settings;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.function.Function;

import static lsfusion.base.BaseUtils.nvl;

public abstract class GridPropertyView<This extends GridPropertyView<This, AddParent>, AddParent extends ServerIdentityObject<AddParent, ?>> extends BaseGroupComponentView<This, AddParent> implements PropertyGroupContainersView<This> {

    public IDGenerator idGen;

    public ToolbarView<This> toolbarSystem;
    public ContainerView filtersContainer;
    public FilterControlsView<This> filterControls;
    public NFOrderSet<FilterView> filters = NFFact.orderSet();
    public ImSet<FilterView> getFilters() {
        return filters.getSet();
    }
    public Iterable<FilterView> getFiltersIt() {
        return filters.getIt();
    }

    public FilterView addFilter(PropertyDrawView property, Version version) {
        FilterView filter = new FilterView(idGen, property);
        filters.add(filter, version);
        filtersContainer.add(filter, version);
        return filter;
    }

    public DefaultFormView.ContainerSet containers;

    @Override
    public DefaultFormView.ContainerSet getContainers() {
        return containers;
    }

    @Override
    public void finalizeAroundInit() {
        super.finalizeAroundInit();

        toolbarSystem.finalizeAroundInit();
        filtersContainer.finalizeAroundInit();
        filterControls.finalizeAroundInit();
        for (FilterView filter : getFiltersIt()) {
            filter.finalizeAroundInit();
        }
    }

    protected NFProperty<Integer> captionHeight = NFFact.property();
    protected NFProperty<Integer> captionCharHeight = NFFact.property();

    protected NFProperty<Boolean> resizeOverflow = NFFact.property(); // actually it is the max height

    protected NFProperty<String> valueClass = NFFact.property();
    protected NFProperty<PropertyObjectEntity> propertyValueClass = NFFact.property();

    protected NFProperty<Integer> lineWidth = NFFact.property();
    protected NFProperty<Integer> lineHeight = NFFact.property();

    protected NFProperty<Boolean> boxed = NFFact.property();

    @Override
    protected boolean hasPropertyComponent() {
        return super.hasPropertyComponent() || getPropertyValueClass() != null;
    }

    public GridPropertyView(IDGenerator idGen, Version version) {
        super(idGen);

        toolbarSystem = new ToolbarView<>(idGen, (This) this);

        filtersContainer = new ContainerView(idGen);
        filtersContainer.setAddParent(this, (Function<This, ContainerView>) aThis -> aThis.filtersContainer);
        if (Settings.get().isVerticalColumnsFiltersContainer()) {
            filtersContainer.setLines(DefaultFormView.GROUP_CONTAINER_LINES_COUNT, version);
        } else {
            filtersContainer.setHorizontal(true, version);
        }
        //disable isReversed optimisation for FILTERS container because children are added after isReversed check
        filtersContainer.setReversed(false, version);

        // behaves weirdly if unset as alignCaptions property sometimes depends on children count, which changes in runtime for filters container
        filtersContainer.setAlignCaptions(false, version);

//        filtersContainer.setLineSize(0);
//        filtersContainer.setCaption(LocalizedString.create(ThreadLocalContext.localize("{form.view.filters.container}")));

        filterControls = new FilterControlsView<>(idGen, (This) this);
    }

    @Override
    public double getDefaultFlex(FormInstanceContext context) {
        return 1;
    }

    @Override
    protected boolean isDefaultShrink(FormInstanceContext context, boolean explicit) {
        ContainerView container = getLayoutParamContainer();
        if(container != null && container.isWrap())
            return true;
        return super.isDefaultShrink(context, explicit);
    }

    @Override
    public FlexAlignment getDefaultAlignment(FormInstanceContext context) {
        return FlexAlignment.STRETCH;
    }

    @Override
    protected boolean isDefaultAlignShrink(FormInstanceContext context, boolean explicit) {
        // actually not needed mostly since for STRETCH align shrink is set, but just in case
        return true;
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        super.customSerialize(pool, outStream);

        Boolean boxed = getBoxed();
        outStream.writeBoolean(boxed != null);
        if(boxed != null)
            outStream.writeBoolean(boxed);

        outStream.writeInt(getCaptionHeight());
        outStream.writeInt(getCaptionCharHeightValue());

        Boolean resizeOverflow = getResizeOverflow();
        outStream.writeBoolean(resizeOverflow != null);
        if(resizeOverflow != null)
            outStream.writeBoolean(resizeOverflow);

        outStream.writeInt(getLineWidthValue());
        outStream.writeInt(getLineHeightValue());

        pool.writeString(outStream, getValueClass());
    }

    public Integer getCaptionHeight() {
        Integer captionHeightValue = captionHeight.get();
        if(captionHeightValue != null)
            return captionHeightValue;

        Integer captionCharHeightValue = getCaptionCharHeight();
        if(captionCharHeightValue != null)
            return -2;

        return -1;
    }
    public void setCaptionHeight(Integer value, Version version) {
        captionHeight.set(value, version);
    }

    public Integer getCaptionCharHeightValue() {
        return nvl(getCaptionCharHeight(), -1);
    }
    public Integer getCaptionCharHeight() {
        return captionCharHeight.get();
    }
    public void setCaptionCharHeight(Integer value, Version version) {
        captionCharHeight.set(value, version);
    }

    public Boolean getResizeOverflow() {
        return resizeOverflow.get();
    }
    public void setResizeOverflow(Boolean value, Version version) {
        resizeOverflow.set(value, version);
    }

    public String getValueClass() {
        return valueClass.get();
    }
    public void setValueClass(String value, Version version) {
        valueClass.set(value, version);
    }

    public PropertyObjectEntity getPropertyValueClass() {
        return propertyValueClass.get();
    }
    public void setPropertyValueClass(PropertyObjectEntity value, Version version) {
        propertyValueClass.set(value, version);
    }

    public int getLineWidthValue() {
        return nvl(getLineWidth(), -1);
    }
    public Integer getLineWidth() {
        return lineWidth.get();
    }
    public void setLineWidth(Integer value, Version version) {
        lineWidth.set(value, version);
    }

    public int getLineHeightValue() {
        return nvl(getLineHeight(), -1);
    }
    public Integer getLineHeight() {
        return lineHeight.get();
    }
    public void setLineHeight(Integer value, Version version) {
        lineHeight.set(value, version);
    }

    public Boolean getBoxed() {
        return boxed.get();
    }
    public void setBoxed(Boolean value, Version version) {
        boxed.set(value, version);
    }

    protected abstract boolean isCustom();

    @Override
    protected int getDefaultWidth(FormInstanceContext context) {
        if (getLineWidth() == null && isCustom())
            return -1;

//        // if we have opposite direction and align shrink, setting default width to zero
//        ContainerView container = getLayoutParamContainer();
//        if(!(container != null && container.isHorizontal()) && isAlignShrink(entity))
//            return 0; // or -1 doesn't matter

        return -2;
    }

    @Override
    protected int getDefaultHeight(FormInstanceContext context) {
        if (getLineHeight() == null && isCustom())
            return -1;

        return -2;
    }

    // copy-constructor
    protected GridPropertyView(This src, ObjectMapping mapping) {
        super(src, mapping);

        idGen = src.idGen;

        toolbarSystem = mapping.get(src.toolbarSystem);
        filtersContainer = mapping.get(src.filtersContainer);
        filterControls = mapping.get(src.filterControls);

        containers = mapping.get(src.containers);
    }

    @Override
    public void extend(This src, ObjectMapping mapping) {
        super.extend(src, mapping);

        mapping.sets(captionHeight, src.captionHeight);
        mapping.sets(captionCharHeight, src.captionCharHeight);
        mapping.sets(resizeOverflow, src.resizeOverflow);
        mapping.sets(valueClass, src.valueClass);
        mapping.sets(lineWidth, src.lineWidth);
        mapping.sets(lineHeight, src.lineHeight);
        mapping.sets(boxed, src.boxed);

        mapping.set(propertyValueClass, src.propertyValueClass);
    }

    @Override
    public void add(This src, ObjectMapping mapping) {
        super.add(src, mapping);

        mapping.add(filters, src.filters);
    }
}
