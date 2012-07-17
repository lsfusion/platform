package platform.gwt.form2.client.form.classes;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.types.TreeModelType;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.grid.events.RecordDoubleClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordDoubleClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.HStack;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;
import platform.gwt.form2.client.MainFrameMessages;
import platform.gwt.form2.client.form.ui.dialog.GModalWindow;
import platform.gwt.form2.client.form.ui.WindowHiddenHandler;
import platform.gwt.view2.classes.GObjectClass;

public class GClassDialog extends GModalWindow {
    private static final MainFrameMessages messages = MainFrameMessages.Instance.get();

    private final GObjectClass baseClass;
    private final GObjectClass defaultClass;
    private final boolean concreate;
    private final ClassChosenHandler classChosenHandler;

    private Tree dataTree;
    private TreeGrid treeGrid;

    private Button btnOk;
    private Button btnCancel;

    private TreeNode defaultClassNode;
    private GObjectClass chosenClass;

    public GClassDialog(GObjectClass baseClass, GObjectClass defaultClass, boolean concreate, final ClassChosenHandler classChosenHandler) {
        super(messages.choseClass());

        this.baseClass = baseClass;
        this.defaultClass = defaultClass;
        this.concreate = concreate;
        this.classChosenHandler = classChosenHandler;

        createTreeGrid();

        createButtons();

        configureLayout();

        bindUIHandlers();
    }

    private void bindUIHandlers() {
        btnOk.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                GClassTreeNode selectedNode = (GClassTreeNode) treeGrid.getSelectedRecord();
                if (selectedNode != null) {
                    chooseClass(selectedNode.objectClass);
                }
            }
        });

        btnCancel.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                chooseClass(null);
            }
        });

        treeGrid.addRecordDoubleClickHandler(new RecordDoubleClickHandler() {
            @Override
            public void onRecordDoubleClick(RecordDoubleClickEvent event) {
                GClassTreeNode selectedNode = (GClassTreeNode) event.getRecord();
                chooseClass(selectedNode.objectClass);
            }
        });

        setWindowHiddenHandler(new WindowHiddenHandler() {
            @Override
            public void onHidden() {
                classChosenHandler.onClassChosen(chosenClass);
            }
        });
    }

    private void chooseClass(GObjectClass chosenClass) {
        if (chosenClass == null || !concreate || chosenClass.concreate) {
            this.chosenClass = chosenClass;
            hide();
            destroy();
        }
    }

    private void configureLayout() {
        HLayout topPane = new HLayout();
        topPane.setWidth100();
        topPane.setHeight("*");
        topPane.setShowEdges(true);
        topPane.addMember(treeGrid);

        HStack bottomPane = new HStack();
        bottomPane.setMargin(10);
        bottomPane.setMembersMargin(5);
        bottomPane.setAlign(Alignment.RIGHT);
        bottomPane.setWidth100();
        bottomPane.setHeight100();
        bottomPane.setAutoHeight();
        bottomPane.addMember(btnOk);
        bottomPane.addMember(btnCancel);

        VLayout mainPane = new VLayout();
        mainPane.addMember(topPane);
        mainPane.addMember(bottomPane);

        addItem(mainPane);
    }

    private void createButtons() {
        btnOk = new Button(messages.ok());
        btnOk.setLayoutAlign(Alignment.RIGHT);

        btnCancel = new Button(messages.cancel());
        btnCancel.setLayoutAlign(Alignment.RIGHT);
    }

    private void createTreeGrid() {
        createDataTree();

        treeGrid = new TreeGrid();
        treeGrid.setWidth100();
        treeGrid.setHeight100();
        treeGrid.setSelectionType(SelectionStyle.SINGLE);
        treeGrid.setShowRollOver(false);
        treeGrid.setCanResizeFields(true);
        treeGrid.setCanSort(false);
        treeGrid.setCanEdit(false);
        treeGrid.setAutoFetchData(true);
        treeGrid.setShowHeader(false);
        treeGrid.setShowHeaderContextMenu(false);
        treeGrid.setShowConnectors(true);
        treeGrid.setShowHeaderMenuButton(false);
        treeGrid.setShowOpenIcons(false);
        treeGrid.setShowDropIcons(false);

        treeGrid.setData(dataTree);

        dataTree.openAll();

        if (defaultClassNode != null) {
            treeGrid.selectRecord(defaultClassNode);
        }
    }

    private void createDataTree() {
        dataTree = new Tree();
        dataTree.setModelType(TreeModelType.CHILDREN);
        dataTree.setRoot(new TreeNode());

        addClassNode(dataTree.getRoot(), baseClass);
    }

    private void addClassNode(TreeNode parentNode, GObjectClass objectClass) {
        TreeNode classNode = new GClassTreeNode(objectClass);
        dataTree.add(classNode, parentNode);

        for (GObjectClass childClass : objectClass.children) {
            addClassNode(classNode, childClass);
        }

        if (objectClass.ID == defaultClass.ID) {
            defaultClassNode = classNode;
        }
    }

    public static GClassDialog showDialog(GObjectClass baseClass, GObjectClass defaultClass, boolean concreate, ClassChosenHandler classChosenHandler) {
        GClassDialog classDlg = new GClassDialog(baseClass, defaultClass, concreate, classChosenHandler);
        classDlg.show();
        return classDlg;
    }
}
