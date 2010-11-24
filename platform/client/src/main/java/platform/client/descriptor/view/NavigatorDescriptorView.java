package platform.client.descriptor.view;

import platform.base.BaseUtils;
import platform.base.context.Lookup;
import platform.client.Main;
import platform.client.descriptor.FormDescriptor;
import platform.client.navigator.ClientNavigator;
import platform.client.navigator.NavigatorTreeNode;
import platform.client.tree.ClientTreeNode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class NavigatorDescriptorView extends JPanel {
    private FormDescriptorView formView;
    private VisualSetupNavigator visualNavigator;

    private List<FormDescriptor> newForms = new ArrayList<FormDescriptor>();
    private Map<Integer, FormDescriptor> editingForms = new HashMap<Integer, FormDescriptor>();

    private JButton previewBtn;
    private JButton saveBtn;
    private JButton cancelBtn;

    public NavigatorDescriptorView(final ClientNavigator clientNavigator) {

        setLayout(new BorderLayout());

        visualNavigator = new VisualSetupNavigator(this, clientNavigator.remoteNavigator);

        formView = new FormDescriptorView();

        previewBtn = new JButton("Предпросмотр формы");
        previewBtn.setEnabled(false);
        previewBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                PreviewDialog dlg = new PreviewDialog(clientNavigator, formView.getForm());
                dlg.setBounds(SwingUtilities.windowForComponent(NavigatorDescriptorView.this).getBounds());
                dlg.setVisible(true);
            }
        });

        saveBtn = new JButton("Сохранить изменения");
        saveBtn.setEnabled(false);
        saveBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                commitChanges();
            }
        });

        cancelBtn = new JButton("Отменить изменения");
        cancelBtn.setEnabled(false);
        cancelBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancelChanges();
            }
        });

        JPanel commandPanel = new JPanel();
        commandPanel.setLayout(new BoxLayout(commandPanel, BoxLayout.X_AXIS));
        commandPanel.add(Box.createRigidArea(new Dimension(5, 5)));
        commandPanel.add(previewBtn);
        commandPanel.add(saveBtn);
        commandPanel.add(Box.createRigidArea(new Dimension(20, 5)));
        commandPanel.add(cancelBtn);
        commandPanel.add(Box.createHorizontalGlue());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(visualNavigator), formView);
        splitPane.setResizeWeight(0.1);

        add(splitPane, BorderLayout.CENTER);
        add(commandPanel, BorderLayout.SOUTH);
    }

    private void cancelChanges() {
        try {
            setupActionButtons(false);
            visualNavigator.cancelNavigatorChanges();

            while (newForms.size() > 0) {
                FormDescriptor form = BaseUtils.last(newForms);
                removeElement(form.ID);
                newForms.remove(form);
            }

            editingForms.clear();

            FormDescriptor currentForm = formView.getForm();
            if (currentForm != null) {
                reopenForm(currentForm.ID);
            }
        } catch (IOException ioe) {
            throw new RuntimeException("Не могу открыть форму.", ioe);
        }
    }

    private void commitChanges() {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataStream = new DataOutputStream(outStream);

            //сохраняем формы
            dataStream.writeInt(editingForms.size());
            for (FormDescriptor form : editingForms.values()) {
                int oldSize = outStream.size();
                form.serialize(dataStream);
                int formSize = outStream.size() - oldSize;
                dataStream.writeInt(formSize);
            }

            //сохраняем новую структуру навигатора
            Map<Integer, List<Integer>> changedElements = getChangedNavigatorElementsChildren();
            dataStream.writeInt(changedElements.size());
            for (Map.Entry<Integer, List<Integer>> entry : changedElements.entrySet()) {
                dataStream.writeInt(entry.getKey());
                dataStream.writeInt(entry.getValue().size());
                for (Integer childID : entry.getValue()) {
                    dataStream.writeInt(childID);
                }
            }

            visualNavigator.remoteNavigator.saveVisualSetup(outStream.toByteArray());

            editingForms.clear();
            FormDescriptor currentForm = formView.getForm();
            if (currentForm != null) {
                reopenForm(currentForm.ID);
            }
        } catch (IOException ioe) {
            throw new RuntimeException("Не могу сохранить форму.", ioe);
        }
    }

    private Map<Integer, List<Integer>> getChangedNavigatorElementsChildren() {
        HashMap<Integer, List<Integer>> result = new HashMap<Integer, List<Integer>>();
        Enumeration<ClientTreeNode> nodes = visualNavigator.getTree().rootNode.depthFirstEnumeration();
        while (nodes.hasMoreElements()) {
            ClientTreeNode node = nodes.nextElement();
            if (node instanceof NavigatorTreeNode) {
                NavigatorTreeNode navigatorNode = (NavigatorTreeNode) node;
                if (navigatorNode.nodeStructureChanged) {
                    navigatorNode.nodeStructureChanged = false;
                    List<Integer> children = new ArrayList<Integer>();
                    for (int i = 0; i < navigatorNode.getChildCount(); ++i) {
                        ClientTreeNode childNode = (ClientTreeNode) navigatorNode.getChildAt(i);
                        if (childNode instanceof NavigatorTreeNode) {
                            NavigatorTreeNode childNavigatorNode = (NavigatorTreeNode) childNode;
                            children.add(childNavigatorNode.navigatorElement.ID);
                        }
                    }
                    result.put(navigatorNode.navigatorElement.ID, children);
                }
            }
        }

        return result;
    }

    public void reopenForm(int ID) throws IOException {
        editingForms.remove(ID);
        openForm(ID);
    }

    public void openForm(int ID) throws IOException {
        if (!editingForms.containsKey(ID)) {
            editingForms.put(ID, FormDescriptor.deserialize(visualNavigator.remoteNavigator.getRichDesignByteArray(ID),
                                                            visualNavigator.remoteNavigator.getFormEntityByteArray(ID)));
        }
        formView.setForm(editingForms.get(ID));
        updateUI();

        setupActionButtons(true);
    }

    public void removeElement(int elementID) {
        FormDescriptor form = editingForms.get(elementID);
        if (form != null) {
            form.getContext().setProperty(Lookup.DELETED_OBJECT_PROPERTY, form);
            editingForms.remove(form.getID());
            newForms.remove(form);
        }
    }

    private void setupActionButtons(boolean enabled) {
        previewBtn.setEnabled(enabled);
        saveBtn.setEnabled(enabled);
        cancelBtn.setEnabled(enabled);
    }

    public boolean isFormChanged(int formID) {
        return editingForms.containsKey(formID);
    }

    public FormDescriptor createAndOpenNewForm() {
        FormDescriptor newForm = new FormDescriptor(Main.generateNewID());

        newForms.add(newForm);
        editingForms.put(newForm.getID(), newForm);
        formView.setForm(newForm);

        return newForm;
    }

    public void cancelForm(int formID) {
        try {
            FormDescriptor currentForm = formView.getForm();
            if (currentForm != null && currentForm.ID == formID) {
                reopenForm(formID);
            } else {
                editingForms.remove(formID);
            }
        } catch (IOException ioe) {
            throw new RuntimeException("Ошибка при отмене формы.");
        }
    }

    public void nodeChanged(NavigatorTreeNode node) {
        saveBtn.setEnabled(true);
        cancelBtn.setEnabled(true);
    }
}
