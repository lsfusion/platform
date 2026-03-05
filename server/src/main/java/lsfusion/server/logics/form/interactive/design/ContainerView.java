package lsfusion.server.logics.form.interactive.design;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.server.base.AppServerImage;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.base.version.ComplexLocation;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.NeighbourComplexLocation;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFComplexOrderSet;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;import lsfusion.server.logics.form.interactive.controller.remote.serialization.FormInstanceContext;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.object.GridView;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.dev.debug.DebugInfo;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ContainerView extends ComponentView {

    public NFComplexOrderSet<ComponentView> children = NFFact.complexOrderSet();

    public LocalizedString caption;
    public String name; // actually used only for icons
    public AppServerImage.Reader image;

    public String captionClass;
    public String valueClass;

    public void setImage(String imagePath, FormView formView) {
        image = AppServerImage.createContainerImage(imagePath, this, formView);
    }

    public AppServerImage getImage(FormView formView, ConnectionContext context) {
        if(this.image != null)
            return this.image.get(context);

        return getDefaultImage(main ? formView : null, context);
    }

    public AppServerImage getDefaultImage(String name, float rankingThreshold, boolean useDefaultIcon, FormView formView, ConnectionContext context) {
        return AppServerImage.createDefaultImage(rankingThreshold,
                name, main ? AppServerImage.Style.FORM : AppServerImage.Style.CONTAINER, getAutoName(formView),
                defaultContext -> useDefaultIcon ? AppServerImage.createContainerImage(AppServerImage.FORM, ContainerView.this, formView).get(defaultContext) : null, context);
    }

    private AppServerImage.AutoName getAutoName(FormView formView) {
        return AppServerImage.getAutoName(main ? () -> formView.getCaption() : () -> caption, main ? () -> formView.entity.getName() : () -> name); // can't be converted to lambda because formView can be null
    }

    private AppServerImage getDefaultImage(FormView formView, ConnectionContext context) {
        return getDefaultImage(AppServerImage.AUTO, main ? Settings.get().getDefaultNavigatorImageRankingThreshold() : Settings.get().getDefaultContainerImageRankingThreshold(),
                 main ? Settings.get().isDefaultNavigatorImage() : Settings.get().isDefaultContainerImage(), formView, context);
    }

    private Boolean collapsible;

    private boolean popup;

    public boolean border;

    public boolean collapsed;

    private DebugInfo.DebugPoint debugPoint;
    private boolean horizontal;
    private boolean tabbed;

    public FlexAlignment childrenAlignment;

    private Boolean grid;
    private Boolean wrap;
    private Boolean alignCaptions;

    public Boolean resizeOverflow;

    public int lines = 1;
    public Boolean reversed = null;
    public Integer lineSize = null;
    public Integer captionLineSize = null;
    public Boolean lineShrink = null;
    private String customDesign = null;

    // temp hack ???
    public GridView recordContainer;

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

    // extras
    public PropertyObjectEntity<?> propertyCaption;
    public PropertyObjectEntity<?> propertyCaptionClass;
    public PropertyObjectEntity<?> propertyValueClass;
    public PropertyObjectEntity<?> propertyImage;
    public PropertyObjectEntity<?> propertyCustomDesign;
    public PropertyObjectEntity<?> getExtra(ContainerViewExtraType type) {
        switch (type) {
            case CAPTION:
                return propertyCaption;
            case CAPTIONCLASS:
                return propertyCaptionClass;
            case VALUECLASS:
                return propertyValueClass;
            case IMAGE:
                return propertyImage;
            case CUSTOM:
                return propertyCustomDesign;
        }
        throw new UnsupportedOperationException();
    }

    public ContainerView() {
    }

    public ContainerView(int ID) {
        this(ID, false);
    }

    public boolean main;
    public ContainerView(int ID, boolean main) {
        super(ID);
        this.main = main;
    }

    public void setCaption(LocalizedString caption) {
        this.caption = caption;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCollapsible(boolean collapsible) {
        this.collapsible = collapsible;
    }
    
    public boolean isCollapsible() {
        if(collapsible != null)
            return collapsible;

        if(Settings.get().isDisableCollapsibleContainers())
            return false;

        return isDefaultCollapsible();
    }

    public void setPopup(boolean popup) {
        this.popup = popup;
        this.collapsed = popup;
    }

    public boolean getBorder() {
        return border; // || hasCaption();
    }

    protected boolean isDefaultCollapsible() {
        return hasCaption();
    }

    private boolean hasCaption() {
        return !PropertyDrawView.hasNoCaption(caption, propertyCaption, null);
    }

    public void setBorder(boolean border) {
        this.border = border;
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = isCollapsible() && collapsed;
    }

    public boolean isTabbed() {
        return tabbed;
    }
    
    public boolean isHorizontal() {
        if(isReversed())
            return !horizontal;

        return horizontal;
    }

    public boolean isGrid() {
        if(grid != null)
            return grid;

        return false;
    }

    public Boolean isWrap() {
        if(wrap != null)
            return wrap;

        return hasLines() || isHorizontal();
    }

    public Boolean getAlignCaptions() {
        if(alignCaptions != null)
            return alignCaptions;

        return isTabbed() ? true : null;
    }

    public FlexAlignment getChildrenAlignment() {
        if(childrenAlignment != null)
            return childrenAlignment;

        return FlexAlignment.START;
    }

    public boolean hasLines() {
        return getLines() > 1;
    }

    public int getLines() {
        if(isReversed())
            return 1;

        return lines;
    }

    private boolean isReversed() {
        return reversed != null ? reversed : (lines > 1 && getChildrenList().size() <= lines && !isGrid());
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
        if(lineShrink != null)
            return lineShrink;

        // if we're shrinking this container, it makes sense to shrink lines too (because they are sort of virtual containers)
        ContainerView container = getLayoutParamContainer();
        boolean horizontal = container != null && container.isHorizontal();
        boolean linesHorizontal = !isHorizontal(); // lines direction
        boolean sameDirection = horizontal == linesHorizontal;
        return sameDirection ? isShrink(context) : isAlignShrink(context);
    }

    private boolean isShrinkDominant(FormInstanceContext context, ContainerView container, boolean horizontal, boolean align) {
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

    // we use Boolean since in desktop and in web there is a different default behaviour
    public Boolean isAlignCaptions() {
        return alignCaptions;
    }

    public void setDebugPoint(DebugInfo.DebugPoint debugPoint) {
        this.debugPoint = debugPoint;
    }

    public void setHorizontal(boolean horizontal) {
        this.horizontal = horizontal;
    }

    public void setTabbed(boolean tabbed) {
        this.tabbed = tabbed;
    }

    public void setChildrenAlignment(FlexAlignment childrenAlignment) {
        this.childrenAlignment = childrenAlignment;
    }

    public void setGrid(Boolean grid) {
        this.grid = grid;
    }

    public void setWrap(Boolean wrap) {
        this.wrap = wrap;
    }

    public void setAlignCaptions(boolean alignCaptions) {
        this.alignCaptions = alignCaptions;
    }

    public void setLines(int lines) {
        this.lines = lines;
    }

    public void setLineSize(Integer lineSize) {
        this.lineSize = lineSize;
    }

    public void setLineShrink(boolean lineShrink) {
        this.lineShrink = lineShrink;
    }

    public void setPropertyCustomDesign(PropertyObjectEntity<?> propertyCustomDesign) {
        this.propertyCustomDesign = propertyCustomDesign;
        this.customDesign = "<div/>"; // now empty means "simple"
    }

    public void setCustomDesign(String customDesign) {
        this.customDesign = customDesign;
    }

    public boolean isCustomDesign() {
        return customDesign != null;
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
        return super.hasPropertyComponent() || propertyCaption != null || propertyCaptionClass != null || propertyValueClass != null || propertyImage != null || propertyCustomDesign != null;
    }
    public void fillPropertyComponents(MExclSet<ComponentView> mComponents) {
        super.fillPropertyComponents(mComponents);

        for (ComponentView child : getChildrenIt())
            child.fillPropertyComponents(mComponents);
    }

    public void fillBaseComponents(MExclSet<ComponentView> mComponents, boolean parentShowIf) {
        for (ComponentView child : getChildrenIt()) {
            if (child instanceof ContainerView) {
                ((ContainerView) child).fillBaseComponents(mComponents, parentShowIf || child.showIf != null);
            } else if (child.showIf != null || (parentShowIf && !(child instanceof PropertyDrawView))) {
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

        pool.writeString(outStream, hasCaption() ? ThreadLocalContext.localize(caption) : null); // optimization
        pool.writeString(outStream, name); // optimization
        AppServerImage.serialize(getImage(pool.context.view, pool.context), outStream, pool);

        pool.writeString(outStream, captionClass);
        pool.writeString(outStream, valueClass);

        outStream.writeBoolean(isCollapsible());
        outStream.writeBoolean(popup);

        pool.writeBoolean(outStream, getBorder());

        pool.writeBoolean(outStream, isHorizontal());
        pool.writeBoolean(outStream, isTabbed());

        pool.writeBoolean(outStream, debugPoint != null);
        if (debugPoint != null) {
            pool.writeString(outStream, debugPoint.path);
            pool.writeString(outStream, debugPoint.toString());
        }

        pool.writeObject(outStream, getChildrenAlignment());
        
        outStream.writeBoolean(isGrid());
        outStream.writeBoolean(isWrap());
        pool.writeObject(outStream, getAlignCaptions());

        outStream.writeBoolean(resizeOverflow != null);
        if(resizeOverflow != null)
            outStream.writeBoolean(resizeOverflow);

        outStream.writeInt(getLines());
        pool.writeInt(outStream, lineSize);
        pool.writeInt(outStream, captionLineSize);
        outStream.writeBoolean(isLineShrink(pool.context));

        outStream.writeBoolean(isCustomDesign());
        if (isCustomDesign())
            pool.writeString(outStream, customDesign);
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        assert false;
//        children = NFFact.finalOrderSet(pool.deserializeList(inStream));

        caption = LocalizedString.create(pool.readString(inStream));
        
        collapsible = inStream.readBoolean();
        popup = inStream.readBoolean();

//        main = pool.readBoolean(inStream); // пока не будем делать, так как надо клиента обновлять

        horizontal = pool.readBoolean(inStream);
        tabbed = pool.readBoolean(inStream);

        childrenAlignment = pool.readObject(inStream);
        
        grid = inStream.readBoolean();
        wrap = inStream.readBoolean();
        alignCaptions = inStream.readBoolean();

        lines = inStream.readInt();
        lineSize = pool.readInt(inStream);
        captionLineSize = pool.readInt(inStream);
        lineShrink = inStream.readBoolean();

        if (inStream.readBoolean())
            customDesign = pool.readString(inStream);
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
        return (caption != null ? ThreadLocalContext.localize(caption) + " " : "") + super.toString();
    }
}
