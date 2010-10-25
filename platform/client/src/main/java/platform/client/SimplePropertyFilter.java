package platform.client;

import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.PropertyObjectDescriptor;
import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.descriptor.property.AbstractNodeDescriptor;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

public class SimplePropertyFilter extends JPanel {
    JList list;
    JTree tree;
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
    boolean all;    //логика сервера
    boolean and;    //and или or для GroupObject

    public SimplePropertyFilter(FormDescriptor form, GroupObjectDescriptor descriptor) {
        super(new BorderLayout());
        this.form = form;
        dialog = new JDialog();
        dialog.setModal(true);
        leftPane = new JPanel(new BorderLayout());
        rightPane = new JPanel(new BorderLayout());
        listModel = new DefaultListModel();
        list = new JList(listModel);
        treeModel = new DefaultTreeModel(getNode("Группы"));
        tree = new JTree(treeModel);

        list.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        leftPane.add(list, BorderLayout.CENTER);
        JPanel radioPanel = new JPanel(new GridLayout(4, 1));
        radioPanel.add(getDirectPanel());
        radioPanel.add(getModifierPanel());
        radioPanel.add(getLogicPanel());
        radioPanel.add(getSetPanel());
        leftPane.add(radioPanel, BorderLayout.SOUTH);
        rightPane.add(new JScrollPane(tree), BorderLayout.CENTER);

        currentElement = form.groupObjects.indexOf(descriptor);
        if (currentElement < 0) {
            currentElement = 0;
        }
        add(leftPane, BorderLayout.WEST);
        add(rightPane, BorderLayout.CENTER);
        updateList();
    }

    public void updateList() {
        listModel.removeAllElements();
        for (GroupObjectDescriptor groupObjectDescriptor : form.groupObjects) {
            listModel.addElement(groupObjectDescriptor.client.toString());
        }
        list.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                updateTree();
            }
        });
        list.getSelectionModel().addSelectionInterval(currentElement, currentElement);
    }

    public void updateTree() {
        int num = 0;
        ArrayList<GroupObjectDescriptor> dependList = new ArrayList<GroupObjectDescriptor>();
        for (GroupObjectDescriptor groupObjectDescriptor : form.groupObjects) {
            if (list.getSelectionModel().isSelectedIndex(num++)) {
                dependList.add(groupObjectDescriptor);
            }
        }
        ArrayList<GroupObjectDescriptor> objectList;
        if (subSetCheckBox.isSelected()) {
            objectList = new ArrayList<GroupObjectDescriptor>(dependList);
        } else {
            objectList = new ArrayList<GroupObjectDescriptor>(form.groupObjects);
        }

        List<PropertyObjectDescriptor> properties = FormDescriptor.getProperties(objectList, Main.remoteLogics, dependList, and);
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        root.removeAllChildren();
        for (PropertyObjectDescriptor prop : properties) {
            if (prop.mapping.isEmpty() && !emptyCheckBox.isSelected()) {
                continue;
            }
            addPropertyToTree(prop);
        }
        treeModel.reload();
    }

    DefaultMutableTreeNode getNode(Object info) {
        return new DefaultMutableTreeNode(info);
    }

    public void addPropertyToTree(PropertyObjectDescriptor prop) {
        Stack stack = new Stack();

        stack.push(prop);
        AbstractNodeDescriptor current = prop.property.parent;

        while (current != null) {
            stack.push(current);
            current = current.parent;
        }

        DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) treeModel.getRoot();
        while (!stack.isEmpty()) {
            boolean found = false;
            int n = currentNode.getChildCount();
            for (int i = 0; i < n; i++) {
                Object userObj = ((DefaultMutableTreeNode) currentNode.getChildAt(i)).getUserObject();
                if (userObj.equals(stack.peek())) {
                    currentNode = ((DefaultMutableTreeNode) currentNode.getChildAt(i));
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
        JPanel checkBoxPanel = new JPanel(new GridLayout(2, 1));
        subSetCheckBox = new JCheckBox("Фильтровать группы объектов");
        emptyCheckBox = new JCheckBox("Отображать пустые");

        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateTree();
            }
        };
        subSetCheckBox.addActionListener(listener);
        emptyCheckBox.addActionListener(listener);
        checkBoxPanel.add(subSetCheckBox);
        checkBoxPanel.add(emptyCheckBox);

        return new TitledPanel("Фильтр выбора", checkBoxPanel);
    }

    private JPanel getLogicPanel() {
        JPanel radioPanel = new JPanel(new GridLayout(1, 3));
        JRadioButton anyButton = new JRadioButton("Or");
        anyButton.setActionCommand("OR");
        anyButton.setSelected(true);
        JRadioButton allButton = new JRadioButton("And");
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

        return new TitledPanel("Логика выбора", radioPanel);
    }

    private JPanel getModifierPanel() {
        JPanel radioPanel = new JPanel(new GridLayout(1, 3));
        JRadioButton anyButton = new JRadioButton("Any");
        anyButton.setActionCommand("ANY");
        anyButton.setSelected(true);
        JRadioButton allButton = new JRadioButton("All");
        allButton.setActionCommand("ALL");

        ButtonGroup group = new ButtonGroup();
        group.add(anyButton);
        group.add(allButton);

        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("ANY")) {
                    all = false;
                } else if (e.getActionCommand().equals("ALL")) {
                    all = true;
                }
                updateTree();
            }
        };
        anyButton.addActionListener(listener);
        allButton.addActionListener(listener);

        radioPanel.add(anyButton);
        radioPanel.add(allButton);

        return new TitledPanel("Тип интерфейса", radioPanel);
    }

    private JPanel getDirectPanel() {
        JPanel radioPanel = new JPanel(new GridLayout(1, 2));
        JButton upButton = new JButton("Верхние");
        upButton.setActionCommand("UP");
        //upButton.setSelected(true);
        JButton downButton = new JButton("Нижние");
        downButton.setActionCommand("DOWN");
        JButton allButton = new JButton("Все");
        allButton.setActionCommand("ALL");

        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("UP")) {
                    changeSelection(0, currentElement);
                } else if (e.getActionCommand().equals("DOWN")) {
                    changeSelection(currentElement, form.groupObjects.size());
                } else if (e.getActionCommand().equals("ALL")) {
                    changeSelection(0, form.groupObjects.size());
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

        return new TitledPanel("Выделение", radioPanel);
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

