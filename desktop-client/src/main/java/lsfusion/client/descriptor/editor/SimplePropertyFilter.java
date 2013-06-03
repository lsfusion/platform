package lsfusion.client.descriptor.editor;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.Main;
import lsfusion.client.descriptor.FormDescriptor;
import lsfusion.client.descriptor.GroupObjectDescriptor;
import lsfusion.client.descriptor.PropertyObjectDescriptor;
import lsfusion.client.descriptor.editor.base.TitledPanel;
import lsfusion.client.descriptor.property.AbstractNodeDescriptor;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class SimplePropertyFilter extends JPanel {
    JList list;
    final JTree tree;
    DefaultListModel listModel;
    DefaultTreeModel treeModel;
    FormDescriptor form;
    JPanel leftPane;
    JPanel rightPane;
    JDialog dialog;
    PropertyObjectDescriptor selectedProperty;
    int currentElement;
    JCheckBox subSetCheckBox; //выбор Only/All
    JCheckBox emptyCheckBox; //отображение пустых свойств(от 0 параметров)
    JCheckBox isAnyCheckBox; //выбор логики all/any
    boolean and;    //and или or для GroupObject
    public List<GroupObjectDescriptor> groupObjects;
    String searchFilter = "";
    
    public SimplePropertyFilter(FormDescriptor form, GroupObjectDescriptor descriptor) {
        super(new BorderLayout(5, 5));
        this.form = form;
        treeModel = new DefaultTreeModel(getNode(ClientResourceBundle.getString("descriptor.editor.group.groups")));
        tree = new JTree(treeModel);
        groupObjects = form.groupObjects;
        currentElement = groupObjects.indexOf(descriptor);
        if (currentElement < 0) {
            currentElement = 0;
        }
        initUI();
    }

    public SimplePropertyFilter(List<GroupObjectDescriptor> groupObjects) {
        super(new BorderLayout(5, 5));
        this.groupObjects = groupObjects;
        treeModel = new DefaultTreeModel(getNode(ClientResourceBundle.getString("descriptor.editor.group.groups")));
        tree = new JTree(treeModel);
        currentElement = -1;
        initUI();
    }

    void initUI() {
        dialog = new JDialog();
        dialog.setModal(true);
        leftPane = new JPanel(new BorderLayout(5, 5));
        rightPane = new JPanel(new BorderLayout(5, 5));
        listModel = new DefaultListModel();
        list = new JList(listModel);

        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        list.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        leftPane.add(new TitledPanel(ClientResourceBundle.getString("descriptor.editor.group.objectgroups"), list), BorderLayout.CENTER);
        JPanel radioPanel = new JPanel(new GridLayout(getPanelCount(), 1, 5, 5));
        if (hasDirectPanel()) {
            radioPanel.add(getDirectPanel());
        }
        radioPanel.add(getLogicPanel());
        radioPanel.add(getSetPanel());
        leftPane.add(radioPanel, BorderLayout.SOUTH);
        rightPane.add(getSearchPanel(), BorderLayout.NORTH);
        rightPane.add(new JScrollPane(tree), BorderLayout.CENTER);

        add(leftPane, BorderLayout.WEST);
        add(rightPane, BorderLayout.CENTER);
        updateList();
    }

    int getPanelCount() {
        return 3;
    }

    boolean hasDirectPanel() {
        return true;
    }

    public void updateList() {
        listModel.removeAllElements();
        for (GroupObjectDescriptor groupObjectDescriptor : groupObjects) {
            listModel.addElement(groupObjectDescriptor.client.toString());
        }
        list.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                updateTree();
            }
        });
        if (currentElement < 0) {
            list.getSelectionModel().addSelectionInterval(0, groupObjects.size());
        } else {
            list.getSelectionModel().addSelectionInterval(currentElement, currentElement);
        }
    }

    public void updateTree() {
        int num = 0;
        ArrayList<GroupObjectDescriptor> dependList = new ArrayList<GroupObjectDescriptor>();
        for (GroupObjectDescriptor groupObjectDescriptor : groupObjects) {
            if (list.getSelectionModel().isSelectedIndex(num++)) {
                dependList.add(groupObjectDescriptor);
            }
        }
        ArrayList<GroupObjectDescriptor> objectList;
        if (subSetCheckBox.isSelected()) {
            objectList = new ArrayList<GroupObjectDescriptor>(dependList);
        } else {
            objectList = new ArrayList<GroupObjectDescriptor>(groupObjects);
        }

        List<PropertyObjectDescriptor> properties = FormDescriptor.getProperties(objectList, Main.remoteLogics, dependList, and, isAnyCheckBox.isSelected());
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        root.removeAllChildren();
        for (PropertyObjectDescriptor prop : properties) {
            if (prop.mapping.isEmpty() && !emptyCheckBox.isSelected()) {
                continue;
            }

            boolean isMask = searchFilter.contains("%") || searchFilter.contains("_");
            if ((isMask && (prop.property.getSID().matches(searchFilter.replace("%", ".*").replace("_", ".?"))))
                    || ("".equals(searchFilter))
                    || (prop.property.getSID().startsWith(searchFilter))
                    || (prop.property.caption.startsWith(searchFilter)))
                addPropertyToTree(prop);
        }
        treeModel.reload();

        expandTree(tree, (DefaultMutableTreeNode)treeModel.getRoot());
    }

    public static void expandTree(JTree tree, DefaultMutableTreeNode start) {
        for (Enumeration children = start.children(); children.hasMoreElements();) {
            DefaultMutableTreeNode dtm = (DefaultMutableTreeNode) children.nextElement();
            if (!dtm.isLeaf()) {
                TreePath tp = new TreePath( dtm.getPath() );
                tree.expandPath(tp);
                expandTree(tree, dtm);
            }
        }
        return;
    }

    DefaultMutableTreeNode getNode(Object info) {
        return new DefaultMutableTreeNode(info);
    }

    private void addPropertyToTree(PropertyObjectDescriptor prop) {
        Stack<Object> stack = new Stack<Object>();

        prop.property.caption += " - " + prop.property.getSID() + " - ";
        stack.push(prop);
        AbstractNodeDescriptor current = prop.property.parent;

        while (current != null) {
            stack.push(current);
            current = current.parent;
        }

        DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) treeModel.getRoot();
        while (!stack.isEmpty()) {
            boolean found = false;
            for (DefaultMutableTreeNode child : Collections.list((Enumeration<DefaultMutableTreeNode>) currentNode.children())) {
                Object userObj = child.getUserObject();
                if (userObj.equals(stack.peek())) {
                    currentNode = child;
                    found = true;
                    break;
                }
            }
            if (!found) {
                DefaultMutableTreeNode node = getNode(stack.peek());
                currentNode.add(node);
                currentNode = node;
            }
            stack.pop();
        }
    }

    private JPanel getSetPanel() {
        JPanel checkBoxPanel = new JPanel(new GridLayout(3, 1));
        subSetCheckBox = new JCheckBox(ClientResourceBundle.getString("descriptor.editor.group.only.specified.groups"));
        emptyCheckBox = new JCheckBox(ClientResourceBundle.getString("descriptor.editor.group.show.empty"));
        isAnyCheckBox = new JCheckBox(ClientResourceBundle.getString("descriptor.editor.group.use.any"));

        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateTree();
            }
        };
        subSetCheckBox.addActionListener(listener);
        emptyCheckBox.addActionListener(listener);
        isAnyCheckBox.addActionListener(listener);
        checkBoxPanel.add(subSetCheckBox);
        checkBoxPanel.add(emptyCheckBox);
        checkBoxPanel.add(isAnyCheckBox);

        return new TitledPanel(ClientResourceBundle.getString("descriptor.editor.group.choice.filter"), checkBoxPanel);
    }

    private JPanel getLogicPanel() {
        JPanel radioPanel = new JPanel(new GridLayout(1, 3));
        JRadioButton anyButton = new JRadioButton(ClientResourceBundle.getString("descriptor.editor.group.at.least.one"));
        anyButton.setActionCommand("OR");
        anyButton.setSelected(true);
        JRadioButton allButton = new JRadioButton(ClientResourceBundle.getString("descriptor.editor.group.allgroups"));
        allButton.setActionCommand("AND");

        ButtonGroup group = new ButtonGroup();
        group.add(anyButton);
        group.add(allButton);

        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("OR")) {
                    and = false;
                } else if (e.getActionCommand().equals("AND")) {
                    and = true;
                }
                updateTree();
            }
        };
        anyButton.addActionListener(listener);
        allButton.addActionListener(listener);

        radioPanel.add(anyButton);
        radioPanel.add(allButton);

        return new TitledPanel(ClientResourceBundle.getString("descriptor.editor.group.choice.logic"), radioPanel);
    }

    private JPanel getSearchPanel() {
        JPanel searchPanel = new JPanel(new GridLayout(1, 1));
        final JTextField textField = new JTextField();
        textField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchFilter = textField.getText();
                updateTree();
            }
        });
        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                searchFilter = textField.getText();
            }
        });

        searchPanel.add(textField);
        return new TitledPanel(ClientResourceBundle.getString("descriptor.editor.search"), searchPanel);
    }

    private JPanel getDirectPanel() {
        JPanel radioPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        JButton upButton = new JButton(ClientResourceBundle.getString("descriptor.editor.group.top"));
        upButton.setActionCommand("UP");
        //upButton.setSelected(true);
        JButton downButton = new JButton(ClientResourceBundle.getString("descriptor.editor.group.bottom"));
        downButton.setActionCommand("DOWN");
        JButton allButton = new JButton(ClientResourceBundle.getString("descriptor.editor.group.all"));
        allButton.setActionCommand("ALL");

        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("UP")) {
                    changeSelection(0, currentElement);
                } else if (e.getActionCommand().equals("DOWN")) {
                    changeSelection(currentElement, groupObjects.size());
                } else if (e.getActionCommand().equals("ALL")) {
                    changeSelection(0, groupObjects.size());
                }
                updateTree();
            }
        };
        upButton.addActionListener(listener);
        downButton.addActionListener(listener);
        allButton.addActionListener(listener);

        radioPanel.add(upButton);
        radioPanel.add(downButton);
        radioPanel.add(allButton);

        return new TitledPanel(ClientResourceBundle.getString("descriptor.editor.group.selection"), radioPanel);
    }

    private void changeSelection(int first, int last) {
        list.getSelectionModel().setSelectionInterval(first, last);
    }

    public PropertyObjectDescriptor getPropertyObject() {
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() < 2) {
                    return;
                }
                if (tree.getSelectionPath() == null) {
                    return;
                }
                DefaultMutableTreeNode element = (DefaultMutableTreeNode) tree.getSelectionPath().getLastPathComponent();
                if (element.getUserObject() instanceof PropertyObjectDescriptor) {
                    selectedProperty = (PropertyObjectDescriptor) element.getUserObject();
                    dialog.setVisible(false);
                } else {
                    selectedProperty = null;
                }
            }
        });
        dialog.setContentPane(this);
        dialog.setBounds(300, 300, 500, 500);
        dialog.setVisible(true);
        return selectedProperty;
    }

}

