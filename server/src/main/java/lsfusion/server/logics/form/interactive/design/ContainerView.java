package lsfusion.server.logics.form.interactive.design;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.identity.IDGenerator;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.server.base.AppServerImage;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.base.version.ComplexLocation;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.NeighbourComplexLocation;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFComplexOrderSet;
import lsfusion.server.base.version.interfaces.NFProperty;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;import lsfusion.server.logics.form.interactive.controller.remote.serialization.FormInstanceContext;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.object.GridView;
import lsfusion.server.logics.form.interactive.design.property.PropertyContainersView;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.dev.debug.DebugInfo;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.function.Function;

import static lsfusion.base.BaseUtils.nvl;

// in fact AddParent is PropertyContainersView, but it requires multiple inheritance
public class ContainerView<AddParent extends IdentityView<AddParent, ?>> extends ComponentView<ContainerView<AddParent>, AddParent> {

    public boolean main;

    private NFProperty<LocalizedString> caption = NFFact.property();
    private NFProperty<String> name = NFFact.property(); // actually used only for icons
    private NFProperty<AppServerImage.Reader> image = NFFact.property();

    private NFProperty<String> valueClass = NFFact.property();
    private NFProperty<String> captionClass = NFFact.property();

    private NFProperty<Boolean> collapsible = NFFact.property();
    private NFProperty<Boolean> popup = NFFact.property();
    private NFProperty<Boolean> border = NFFact.property();
    private NFProperty<Boolean> collapsed = NFFact.property();

    private DebugInfo.DebugPoint debugPoint;
    private NFProperty<Boolean> horizontal = NFFact.property();
    private NFProperty<Boolean> tabbed = NFFact.property();

    private NFProperty<FlexAlignment> childrenAlignment = NFFact.property();

    private NFProperty<Boolean> grid = NFFact.property();
    private NFProperty<Boolean> wrap = NFFact.property();
    private NFProperty<Boolean> alignCaptions = NFFact.property();

    private NFProperty<Boolean> resizeOverflow = NFFact.property();

    private NFProperty<Integer> lines = NFFact.property();
    private NFProperty<Boolean> reversed = NFFact.property();
    private NFProperty<Integer> lineSize = NFFact.property();
    private NFProperty<Integer> captionLineSize = NFFact.property();
    private NFProperty<Boolean> lineShrink = NFFact.property();
    private NFProperty<String> customDesign = NFFact.property();

    private NFProperty<PropertyObjectEntity> propertyCaption = NFFact.property();
    private NFProperty<PropertyObjectEntity> propertyCaptionClass = NFFact.property();
    private NFProperty<PropertyObjectEntity> propertyValueClass = NFFact.property();
    private NFProperty<PropertyObjectEntity> propertyImage = NFFact.property();
    private NFProperty<PropertyObjectEntity> propertyCustomDesign = NFFact.property();

    public NFComplexOrderSet<ComponentView> children = NFFact.complexOrderSet();

    // temp hack ???
    public GridView recordContainer;

    public void setImage(String imagePath, FormView formView, Version version) {
        setName(AppServerImage.createContainerImage(imagePath, this, formView), version);
    }

    public AppServerImage getImage(FormView formView, ConnectionContext context) {
        AppServerImage.Reader image = getImage();
        if(image != null)
            return image.get(context);

        return getDefaultImage(main ? formView : null, context);
    }

    public AppServerImage getDefaultImage(String name, float rankingThreshold, boolean useDefaultIcon, FormView formView, ConnectionContext context) {
        return AppServerImage.createDefaultImage(rankingThreshold,
                name, main ? AppServerImage.Style.FORM : AppServerImage.Style.CONTAINER, getAutoName(formView),
                defaultContext -> useDefaultIcon ? AppServerImage.createContainerImage(AppServerImage.FORM, ContainerView.this, formView).get(defaultContext) : null, context);
    }

    private AppServerImage.AutoName getAutoName(FormView formView) {
        return AppServerImage.getAutoName(this::getCaption, main ? () -> formView.entity.getName() : this::getName); // can't be converted to lambda because formView can be null
    }

    private AppServerImage getDefaultImage(FormView formView, ConnectionContext context) {
        return getDefaultImage(AppServerImage.AUTO, main ? Settings.get().getDefaultNavigatorImageRankingThreshold() : Settings.get().getDefaultContainerImageRankingThreshold(),
                 main ? Settings.get().isDefaultNavigatorImage() : Settings.get().isDefaultContainerImage(), formView, context);
    }

    public void add(ComponentView component, ComplexLocation<ComponentView> location, Version version) {
        if(addOrMoveChecked(component, location, version) != null)
            throw new RuntimeException("Incorrect neighbour");
    }

    public <E extends Exception> ComponentView addOrMoveChecked(ComponentView component, ComplexLocation<ComponentView> location, Version version) throws E {
        ComponentView incorrectNeighbour = checkNeighbour(component, location, version);
        if(incorrectNeighbour != null)
            return incorrectNeighbour;

        addOrMove(component, location, version);
        return null;
    }
    public void addOrMove(ComponentView component, ComplexLocation<ComponentView> location, Version version) {
        component.removeFromParent(version);
        children.add(component, location, version);

        component.setContainer(this, version);
    }

    public <E extends Exception> ComponentView checkNeighbour(ComponentView component, ComplexLocation<ComponentView> location, Version version) throws E {
        if(location instanceof NeighbourComplexLocation) {
            NeighbourComplexLocation<ComponentView> neighbourLocation = (NeighbourComplexLocation<ComponentView>) location;

            ComponentView neighbour = neighbourLocation.element;
            if (!equals(neighbour.getNFContainer(version)))
                return neighbour;
        }
        return null;
    }

    @Override
    public ComponentView getHiddenContainer() {
        ComponentView container = super.getHiddenContainer();
        if(recordContainer != null) {
            assert container == null;
            return recordContainer;
        }
        return container;
    }

    public PropertyObjectEntity<?> getExtra(ContainerViewExtraType type) {
        switch (type) {
            case CAPTION:
                return getPropertyCaption();
            case CAPTIONCLASS:
                return getPropertyCaptionClass();
            case VALUECLASS:
                return getPropertyValueClass();
            case IMAGE:
                return getPropertyImage();
            case CUSTOM:
                return getPropertyCustomDesign();
        }
        throw new UnsupportedOperationException();
    }

    private boolean hasCaption() {
        return !PropertyDrawView.hasNoCaption(getCaption(), getPropertyCaption(), null);
    }
    private boolean hasNFCaption(Version version) {
        return !PropertyDrawView.hasNoCaption(getNFCaption(version), getNFPropertyCaption(version), null);
    }

    public boolean hasLines() {
        return getLines(true) > 1;
    }

    private boolean isReversed() {
        Boolean reversed = getReversed();
        return reversed != null ? reversed : (getLines(false) > 1 && getChildrenList().size() <= getLines(false) && !isGrid());
    }

    @Override
    public boolean isDefaultShrink(FormInstanceContext context, boolean explicit) {
        ContainerView container = getLayoutParamContainer();
        boolean horizontal = container != null && container.isHorizontal();

        if(isShrinkedAutoSizedWrap(context, horizontal))
            return true;

        if(!explicit && container != null && container.isWrap() && isShrinkDominant(context, container, horizontal, false))
            return true;

        return super.isDefaultShrink(context, explicit);
    }

    public boolean isDefaultAlignShrink(FormInstanceContext context, boolean explicit) {
        ContainerView container = getLayoutParamContainer();
        boolean horizontal = container != null && container.isHorizontal();
        if(isShrinkedAutoSizedWrap(context, !horizontal))
            return true;

        if(!explicit && container != null && isShrinkDominant(context, container, !horizontal, true))
            return true;

        return super.isDefaultAlignShrink(context, explicit);
    }

    public boolean isLineShrink(FormInstanceContext context) {
        Boolean lineShrink = getLineShrink();
        if(lineShrink != null)
            return lineShrink;

        // if we're shrinking this container, it makes sense to shrink lines too (because they are sort of virtual containers)
        ContainerView container = getLayoutParamContainer();
        boolean horizontal = container != null && container.isHorizontal();
        boolean linesHorizontal = !isHorizontal(); // lines direction
        boolean sameDirection = horizontal == linesHorizontal;
        return sameDirection ? isShrink(context) : isAlignShrink(context);
    }

    private boolean isShrinkDominant(FormInstanceContext context, ContainerView<?> container, boolean horizontal, boolean align) {
        ContainerView upperContainer = container.getLayoutParamContainer();
        boolean upperHorizontal = upperContainer != null && upperContainer.isHorizontal();
        if((horizontal == upperHorizontal ? container.isShrink(context) : container.isAlignShrink(context))) {
            // checking siblings if there are more
            int shrinked = 0;
            int notShrinked = 0;
            for(ComponentView child : container.getChildrenIt())
                if(align ? child.isAlignShrink(context, true) : child.isShrink(context, true))
                    shrinked++;
                else
                    notShrinked++;
            if(shrinked > notShrinked)
                 return true;
        }
        return false;
    }

    // if we have cascade shrinking (with auto size) and some wrap at some point, consider that we want shrink
    // otherwise shrinking will lead to more scrolls in lower containers
    // however we can use simple shrink check
    protected boolean isShrinkedAutoSizedWrap(FormInstanceContext context, boolean horizontal) {
        if ((horizontal ? getWidth(context) : getHeight(context)) != -1) // if we have fixed size than there is no wrap problem
            return false;

        boolean thisHorizontal = isHorizontal();
        // now there are several heuristics at the web client changing the default behaviour, and disabling wrap
        // most of them are grid related, so we just disable shrink in grid for now
        if (isWrap() && !isGrid()) {
            boolean wrapHorizontal = (thisHorizontal == !hasLines());
            return wrapHorizontal == horizontal; // if there is wrap and it's in required direction that's what we are looking for
            // important if it's wrong direction wrap, we should not use children since it will break this heuristics (it doesn't make sense when wrap "goes" to the upper containers)
        }

        boolean sameDirection = horizontal == thisHorizontal;
        for (ComponentView child : getChildrenList())
            if(child instanceof ContainerView) {
                ContainerView containerChild = (ContainerView) child;
                if ((sameDirection ? containerChild.isShrink(context, true) : child.isAlignShrink(context, true)) && containerChild.isShrinkedAutoSizedWrap(context, horizontal))
                    return true;
            }

        return false;
    }

    public boolean isCustomDesign() {
        return getCustomDesign() != null;
    }

    @Override
    public ComponentView findById(int id) {
        ComponentView result = super.findById(id);
        if(result!=null) return result;

        for(ComponentView child : getChildrenIt()) {
            result = child.findById(id);
            if(result!=null) return result;
        }

        return null;
    }

    public void add(ComponentView comp, Version version) {
        add(comp, ComplexLocation.DEFAULT(), version);
    }
    public void addFirst(ComponentView comp, Version version) {
        add(comp, ComplexLocation.FIRST(), version);
    }
    public void addLast(ComponentView comp, Version version) {
        add(comp, ComplexLocation.LAST(), version);
    }

    public void addBefore(ComponentView comp, ComponentView compBefore, Version version) {
        add(comp, ComplexLocation.BEFORE(compBefore), version);
    }

    public void addAfter(ComponentView comp, ComponentView compAfter, Version version) {
        add(comp, ComplexLocation.AFTER(compAfter), version);
    }

    protected boolean hasPropertyComponent() {
        return super.hasPropertyComponent() || getPropertyCaption() != null || getPropertyCaptionClass() != null || getPropertyValueClass() != null || getPropertyImage() != null || getPropertyCustomDesign() != null;
    }
    public void fillPropertyComponents(MExclSet<ComponentView> mComponents) {
        super.fillPropertyComponents(mComponents);

        for (ComponentView child : getChildrenIt())
            child.fillPropertyComponents(mComponents);
    }

    public void fillBaseComponents(MExclSet<ComponentView> mComponents, boolean parentShowIf) {
        for (ComponentView child : getChildrenIt()) {
            if (child instanceof ContainerView) {
                ((ContainerView) child).fillBaseComponents(mComponents, parentShowIf || child.getShowIf() != null);
            } else if (child.getShowIf() != null || (parentShowIf && !(child instanceof PropertyDrawView))) {
                mComponents.exclAdd(child);
            }
        }
    }

    public boolean isAncestorOf(ComponentView container) {
        return container != null && (super.isAncestorOf(container) || isAncestorOf(container.getHiddenContainer()));
    }

    public boolean isNFAncestorOf(ComponentView container, Version version) {
        return container != null && (super.isNFAncestorOf(container, version) || isNFAncestorOf(container.getNFContainer(version), version));
    }

    ImList<ComponentView> lazyChildren;
    private ImList<ComponentView> getLazyChildren() {
        if (lazyChildren == null) {
            lazyChildren = children.getList().filterList(child -> child.getContainer() == ContainerView.this);
        }
        return lazyChildren;
    }

    public Iterable<ComponentView> getChildrenIt() {
        return getLazyChildren();
    }
    public ImList<ComponentView> getChildrenList() {
        return getLazyChildren();
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        super.customSerialize(pool, outStream);

        pool.serializeCollection(outStream, getChildrenList());

        pool.writeString(outStream, hasCaption() ? ThreadLocalContext.localize(getCaption()) : null); // optimization
        pool.writeString(outStream, getName()); // optimization
        AppServerImage.serialize(getImage(pool.context.view, pool.context), outStream, pool);

        pool.writeString(outStream, getCaptionClass());
        pool.writeString(outStream, getValueClass());

        outStream.writeBoolean(isCollapsible());
        outStream.writeBoolean(isPopup());

        pool.writeBoolean(outStream, isBorder());

        pool.writeBoolean(outStream, isHorizontal());
        pool.writeBoolean(outStream, isTabbed());

        DebugInfo.DebugPoint debugPoint = getDebugPoint();
        pool.writeBoolean(outStream, debugPoint != null);
        if (debugPoint != null) {
            pool.writeString(outStream, debugPoint.path);
            pool.writeString(outStream, debugPoint.toString());
        }

        pool.writeObject(outStream, getChildrenAlignment());

        outStream.writeBoolean(isGrid());
        outStream.writeBoolean(isWrap());
        pool.writeObject(outStream, getAlignCaptions());

        Boolean resizeOverflow = getResizeOverflow();
        outStream.writeBoolean(resizeOverflow != null);
        if(resizeOverflow != null)
            outStream.writeBoolean(resizeOverflow);

        outStream.writeInt(getLines(true));
        pool.writeInt(outStream, getLineSize());
        pool.writeInt(outStream, getCaptionLineSize());
        outStream.writeBoolean(isLineShrink(pool.context));

        outStream.writeBoolean(isCustomDesign());
        if (isCustomDesign())
            pool.writeString(outStream, getCustomDesign());
    }

    public LocalizedString getCaption() {
        return caption.get();
    }
    public LocalizedString getNFCaption(Version version) {
        return caption.getNF(version);
    }
    public void setCaption(LocalizedString value, Version version) {
        caption.set(value, version);
    }

    public String getName() {
        return name.get();
    }
    public void setName(String value, Version version) {
        name.set(value, version);
    }

    public AppServerImage.Reader getImage() {
        return image.get();
    }
    public AppServerImage.Reader getNFImage(Version version) {
        return image.getNF(version);
    }
    public void setName(AppServerImage.Reader value, Version version) {
        image.set(value, version);
    }

    public DebugInfo.DebugPoint getDebugPoint() {
        return debugPoint;
    }
    public void setDebugPoint(DebugInfo.DebugPoint value) {
        debugPoint = value;
    }

    public String getValueClass() {
        return valueClass.get();
    }
    public void setValueClass(String value, Version version) {
        valueClass.set(value, version);
    }

    public String getCaptionClass() {
        return captionClass.get();
    }
    public void setCaptionClass(String value, Version version) {
        captionClass.set(value, version);
    }

    public boolean isCollapsible() {
        Boolean collapsibleValue = collapsible.get();
        if(collapsibleValue != null)
            return collapsibleValue;

        if(Settings.get().isDisableCollapsibleContainers())
            return false;

        return isDefaultCollapsible();
    }
    public boolean isNFCollapsible(Version version) {
        Boolean collapsibleValue = collapsible.getNF(version);
        if(collapsibleValue != null)
            return collapsibleValue;

        if(Settings.get().isDisableCollapsibleContainers())
            return false;

        return isNFDefaultCollapsible(version);
    }
    protected boolean isDefaultCollapsible() {
        return hasCaption();
    }
    protected boolean isNFDefaultCollapsible(Version version) {
        return hasNFCaption(version);
    }
    public void setCollapsible(Boolean value, Version version) {
        collapsible.set(value, version);
    }

    public boolean isPopup() {
        return nvl(popup.get(), false);
    }
    public void setPopup(boolean value, Version version) {
        popup.set(value, version);
        setCollapsed(value, version);
    }

    public boolean isBorder() {
        return nvl(border.get(), false);
    }
    public void setBorder(boolean value, Version version) {
        border.set(value, version);
    }

    public boolean isCollapsed() {
        return nvl(collapsed.get(), false);
    }
    public void setCollapsed(boolean value, Version version) {
        if(isNFCollapsible(version))
            collapsed.set(value, version);
    }

    public boolean isHorizontal() {
        Boolean isHorizontal = nvl(horizontal.get(), false);
        return isReversed() != isHorizontal;
    }
    public void setHorizontal(boolean value, Version version) {
        horizontal.set(value, version);
    }

    public boolean isTabbed() {
        return nvl(tabbed.get(), false);
    }
    public void setTabbed(boolean value, Version version) {
        tabbed.set(value, version);
    }

    public FlexAlignment getChildrenAlignment() {
        return nvl(childrenAlignment.get(), FlexAlignment.START);
    }
    public void setChildrenAlignment(FlexAlignment value, Version version) {
        childrenAlignment.set(value, version);
    }

    public boolean isGrid() {
        return nvl(grid.get(), false);
    }
    public void setGrid(Boolean value, Version version) {
        grid.set(value, version);
    }

    public boolean isWrap() {
        Boolean isWrap = wrap.get();
        if (isWrap != null)
            return isWrap;
        return hasLines() || isHorizontal();
    }
    public void setWrap(Boolean value, Version version) {
        wrap.set(value, version);
    }

    public Boolean getAlignCaptions() {
        Boolean isAlignCaptions = alignCaptions.get();
        if (isAlignCaptions != null)
            return isAlignCaptions;
        return isTabbed() ? true : null;
    }
    public void setAlignCaptions(Boolean value, Version version) {
        alignCaptions.set(value, version);
    }

    public Boolean getResizeOverflow() {
        return resizeOverflow.get();
    }
    public void setResizeOverflow(Boolean value, Version version) {
        resizeOverflow.set(value, version);
    }

    public int getLines(boolean rec) {
        if (rec && isReversed())
            return 1;
        return nvl(lines.get(), 1);
    }
    public void setLines(Integer value, Version version) {
        lines.set(value, version);
    }

    public Boolean getReversed() {
        return reversed.get();
    }
    public void setReversed(Boolean value, Version version) {
        reversed.set(value, version);
    }

    public Integer getLineSize() {
        return lineSize.get();
    }
    public void setLineSize(Integer value, Version version) {
        lineSize.set(value, version);
    }

    public Integer getCaptionLineSize() {
        return captionLineSize.get();
    }
    public void setCaptionLineSize(Integer value, Version version) {
        captionLineSize.set(value, version);
    }

    public Boolean getLineShrink() {
        return lineShrink.get();
    }
    public void setLineShrink(Boolean value, Version version) {
        lineShrink.set(value, version);
    }

    public String getCustomDesign() {
        return customDesign.get();
    }
    public void setCustomDesign(String value, Version version) {
        customDesign.set(value, version);
    }

    public PropertyObjectEntity getPropertyCaption() {
        return propertyCaption.get();
    }
    public PropertyObjectEntity getNFPropertyCaption(Version version) {
        return propertyCaption.getNF(version);
    }
    public void setPropertyCaption(PropertyObjectEntity value, Version version) {
        propertyCaption.set(value, version);
    }

    public PropertyObjectEntity getPropertyCaptionClass() {
        return propertyCaptionClass.get();
    }
    public void setPropertyCaptionClass(PropertyObjectEntity value, Version version) {
        propertyCaptionClass.set(value, version);
    }

    public PropertyObjectEntity getPropertyValueClass() {
        return propertyValueClass.get();
    }
    public void setPropertyValueClass(PropertyObjectEntity value, Version version) {
        propertyValueClass.set(value, version);
    }

    public PropertyObjectEntity getPropertyImage() {
        return propertyImage.get();
    }
    public void setPropertyImage(PropertyObjectEntity value, Version version) {
        propertyImage.set(value, version);
    }

    public PropertyObjectEntity getPropertyCustomDesign() {
        return propertyCustomDesign.get();
    }
    public void setPropertyCustomDesign(PropertyObjectEntity value, Version version) {
        propertyCustomDesign.set(value, version);
        setCustomDesign("<div/>", version); // now empty means "simple"
    }

    @Override
    public void finalizeAroundInit() {
        super.finalizeAroundInit();

        for(ComponentView child : getChildrenIt())
            child.finalizeAroundInit();
    }

    @Override
    public void prereadAutoIcons(FormView formView, ConnectionContext context) {
        getImage(formView, context);
        for(ComponentView child : getChildrenIt())
            child.prereadAutoIcons(formView, context);
    }

    @Override
    public String toString() {
        return ThreadLocalContext.localize(getCaption()) + " " + super.toString();
    }

    public int ID;

    @Override
    public int getID() {
        return ID;
    }

    public ContainerView(IDGenerator idGen, DebugInfo.DebugPoint debugPoint) {
        this.ID = idGen.id();
        this.debugPoint = debugPoint;
    }

    // copy-constructor
    protected ContainerView(ContainerView<AddParent> src, ObjectMapping mapping) {
        super(src, mapping);

        ID = mapping.id();
        debugPoint = src.debugPoint;

        main = src.main;

        recordContainer = mapping.get(src.recordContainer);
        addParent = mapping.get(src.addParent);
        addChild = src.addChild;
    }

    @Override
    public void extend(ContainerView<AddParent> src, ObjectMapping mapping) {
        super.extend(src, mapping);

        mapping.sets(caption, src.caption);
        mapping.sets(name, src.name);
        mapping.sets(image, src.image);

        mapping.sets(valueClass, src.valueClass);
        mapping.sets(captionClass, src.captionClass);

        mapping.sets(collapsible, src.collapsible);
        mapping.sets(popup, src.popup);
        mapping.sets(border, src.border);
        mapping.sets(collapsed, src.collapsed);

        mapping.sets(horizontal, src.horizontal);
        mapping.sets(tabbed, src.tabbed);

        mapping.sets(childrenAlignment, src.childrenAlignment);

        mapping.sets(grid, src.grid);
        mapping.sets(wrap, src.wrap);
        mapping.sets(alignCaptions, src.alignCaptions);

        mapping.sets(resizeOverflow, src.resizeOverflow);

        mapping.sets(lines, src.lines);
        mapping.sets(reversed, src.reversed);
        mapping.sets(lineSize, src.lineSize);
        mapping.sets(captionLineSize, src.captionLineSize);
        mapping.sets(lineShrink, src.lineShrink);
        mapping.sets(customDesign, src.customDesign);

        mapping.set(propertyCaption, src.propertyCaption);
        mapping.set(propertyCaptionClass, src.propertyCaptionClass);
        mapping.set(propertyValueClass, src.propertyValueClass);
        mapping.set(propertyImage, src.propertyImage);
        mapping.set(propertyCustomDesign, src.propertyCustomDesign);
    }

    @Override
    public void add(ContainerView<AddParent> src, ObjectMapping mapping) {
        super.add(src, mapping);

        mapping.add(children, src.children);
    }

    public IdentityView addParent;
    public Function<IdentityView, ContainerView<?>> addChild;
    public <PC extends IdentityView> void setAddParent(PC addParent, Function<PC, ContainerView<?>> addChild) {
        this.addParent = addParent;
        this.addChild = (Function<IdentityView, ContainerView<?>>) addChild;
    }
    public <PC extends PropertyContainersView<PC>> void setAddParentPC(PC addParent, Function<PC, ContainerView<?>> addChild) {
        this.addParent = (IdentityView) addParent;
        this.addChild = BaseUtils.immutableCast(addChild);
    }
    @Override
    public AddParent getAddParent(ObjectMapping mapping) {
        if(mapping.extend) // merge default containers - form:main, group: record, filters, property groups
            return (AddParent) addParent;
        return null;
    }
    @Override
    public ContainerView<AddParent> getAddChild(AddParent serverIdentityObject, ObjectMapping mapping) {
        return (ContainerView<AddParent>) addChild.apply(serverIdentityObject);
    }

    @Override
    public ContainerView<AddParent> copy(ObjectMapping mapping) {
        return new ContainerView<>(this, mapping);
    }
}
