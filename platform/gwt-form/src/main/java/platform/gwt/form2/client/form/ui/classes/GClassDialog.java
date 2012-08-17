package platform.gwt.form2.client.form.ui.classes;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;
import platform.gwt.form2.client.MainFrameMessages;
import platform.gwt.form2.client.form.ui.dialog.GModalWindow;
import platform.gwt.form2.client.form.ui.dialog.WindowHiddenHandler;
import platform.gwt.form2.shared.view.classes.GObjectClass;

public class GClassDialog extends GModalWindow {
    private static final MainFrameMessages messages = MainFrameMessages.Instance.get();

    private final GObjectClass baseClass;
    private final GObjectClass defaultClass;
    private final boolean concreate;
    private final ClassChosenHandler classChosenHandler;

    private Tree tree;

    private Button btnOk;
    private Button btnCancel;

    private TreeItem defaultClassNode;
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
                TreeItem selectedNode = tree.getSelectedItem();
                if (selectedNode != null) {
                    chooseClass((GObjectClass) selectedNode.getUserObject());
                }
            }
        });

        btnCancel.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                chooseClass(null);
            }
        });

        //todo:
//        treeGrid.addRecordDoubleClickHandler(new RecordDoubleClickHandler() {
//            @Override
//            public void onRecordDoubleClick(RecordDoubleClickEvent event) {
//                GClassTreeNode selectedNode = (GClassTreeNode) event.getRecord();
//                chooseClass(selectedNode.objectClass);
//            }
//        });

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
        }
    }

    private void configureLayout() {
        ResizeLayoutPanel topPane = new ResizeLayoutPanel();
        topPane.setWidth("500px");
        topPane.setHeight("500px");
        topPane.setWidget(new ScrollPanel(tree));

        FlowPanel bottomPane = new FlowPanel();
        bottomPane.add(btnOk);
        bottomPane.add(btnCancel);

        VerticalPanel mainPane = new VerticalPanel();
        mainPane.add(topPane);
        mainPane.add(bottomPane);
        mainPane.setCellHorizontalAlignment(bottomPane, HasAlignment.ALIGN_RIGHT);

        setWidget(mainPane);
    }

    private void createButtons() {
        btnOk = new Button(messages.ok());
        btnCancel = new Button(messages.cancel());
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

    public static GClassDialog showDialog(GObjectClass baseClass, GObjectClass defaultClass, boolean concreate, ClassChosenHandler classChosenHandler) {
        GClassDialog classDlg = new GClassDialog(baseClass, defaultClass, concreate, classChosenHandler);
        classDlg.center();
        return classDlg;
    }
}
