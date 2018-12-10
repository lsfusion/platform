package lsfusion.gwt.form.client.form.ui.classes;

import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.form.shared.view.classes.GObjectClass;

import static com.google.gwt.safehtml.shared.SafeHtmlUtils.fromString;
import static lsfusion.gwt.base.client.GwtClientUtils.stopPropagation;

public abstract class ClassTreePanel extends Composite {
    private GObjectClass baseClass;
    private GObjectClass defaultClass;

    private ResizeLayoutPanel mainPane;
    private Tree tree;

    private TreeItem defaultClassNode;

    public ClassTreePanel(GObjectClass baseClass, GObjectClass defaultClass) {
        this.baseClass = baseClass;
        this.defaultClass = defaultClass;

        createTreeGrid();

        configureLayout();

        initWidget(mainPane);
    }

    public void configureLayout() {
        mainPane = new ResizeLayoutPanel();
        mainPane.setWidget(new ScrollPanel(tree));
    }

    private void createTreeGrid() {
        tree = new ClassTree();
        tree.setAnimationEnabled(false);

        addClassNode(null, baseClass);

        tree.setSelectedItem(defaultClassNode);

        if (defaultClassNode != null) {
            tree.setSelectedItem(defaultClassNode);
        }
    }

    private void addClassNode(TreeItem parentNode, GObjectClass objectClass) {
        final TreeItem classNode = parentNode == null
                                   ? tree.addItem(fromString(objectClass.caption))
                                   : parentNode.addItem(fromString(objectClass.caption));
        classNode.setUserObject(objectClass);

        for (GObjectClass childClass : objectClass.children) {
            addClassNode(classNode, childClass);
        }

        classNode.setState(true);

        if (objectClass.ID == defaultClass.ID) {
            defaultClassNode = classNode;
        }
    }

    public GObjectClass getSelectedClass() {
        TreeItem selectedNode = tree.getSelectedItem();
        if (selectedNode != null) {
            return (GObjectClass) selectedNode.getUserObject();
        }
        return null;
    }

    public abstract void classChosen();

    class ClassTree extends Tree {
        public ClassTree() {
            super();
            sinkEvents(Event.ONDBLCLICK);
        }

        @Override
        public void onBrowserEvent(Event event) {
            if (event.getTypeInt() == Event.ONDBLCLICK) {
                stopPropagation(event);
                classChosen();
            }
            super.onBrowserEvent(event);
        }
    }
}
