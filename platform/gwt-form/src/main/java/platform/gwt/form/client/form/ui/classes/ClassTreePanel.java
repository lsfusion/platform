package platform.gwt.form.client.form.ui.classes;

import com.google.gwt.user.client.ui.*;
import platform.gwt.form.shared.view.classes.GObjectClass;

public class ClassTreePanel extends Composite {
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
        tree = new Tree();
        tree.setAnimationEnabled(false);

        addClassNode(null, baseClass);

        tree.setSelectedItem(defaultClassNode);

        if (defaultClassNode != null) {
            tree.setSelectedItem(defaultClassNode);
        }
    }

    private void addClassNode(TreeItem parentNode, GObjectClass objectClass) {
        final TreeItem classNode = parentNode == null ? tree.addItem(objectClass.caption) : parentNode.addItem(objectClass.caption);
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
}
