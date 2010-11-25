package platform.client.descriptor.view;

import platform.base.context.IncrementView;
import platform.base.context.Lookup;
import platform.client.Main;
import platform.client.descriptor.FormDescriptor;
import platform.client.navigator.ClientNavigator;
import platform.client.navigator.ClientNavigatorElement;
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

    private Map<Integer, FormDescriptor> newForms = new HashMap<Integer, FormDescriptor>();
    private Map<Integer, ClientNavigatorElement> newElements = new HashMap<Integer, ClientNavigatorElement>();
    private Map<Integer, FormDescriptor> changedForms = new HashMap<Integer, FormDescriptor>();

    private JButton previewBtn;
    private JButton saveBtn;
    private JButton cancelBtn;
    private boolean hasChangedNodes = false;

    private final IncrementView captionUpdater = new IncrementView() {
        public void update(Object updateObject, String updateField) {
            if (updateObject instanceof FormDescriptor) {
                FormDescriptor form = (FormDescriptor) updateObject;
                Enumeration<ClientTreeNode> nodes = visualNavigator.getTree().rootNode.depthFirstEnumeration();
                while (nodes.hasMoreElements()) {
                    ClientTreeNode node = nodes.nextElement();
                    if (node instanceof NavigatorTreeNode) {
                        NavigatorTreeNode navigatorNode = (NavigatorTreeNode) node;
                        if (navigatorNode.navigatorElement.ID == form.ID) {
                            navigatorNode.navigatorElement.setCaption(form.getCaption());
                            visualNavigator.getTree().updateUI();
                            break;
                        }
                    }
                }
            }
        }
    };

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
            visualNavigator.cancelNavigatorChanges();

            while (newForms.size() > 0) {
                FormDescriptor form = newForms.values().iterator().next();
                removeElement(form.ID);
                newForms.remove(form.ID);
            }

            changedForms.clear();

            FormDescriptor currentForm = formView.getForm();
            if (currentForm != null) {
                openForm(currentForm.ID);
            }
        } catch (IOException e) {
            throw new RuntimeException("Не могу открыть форму.", e);
        }

        hasChangedNodes = false;

        setupActionButtons();

        updateUI();
    }

    private void commitChanges() {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataStream = new DataOutputStream(outStream);

            //сохраняем элементы
            dataStream.writeInt(newElements.size());
            for (ClientNavigatorElement element : newElements.values()) {
                int bytesWritten = outStream.size();
                element.serialize(dataStream);
                int elementSize = outStream.size() - bytesWritten;
                dataStream.writeInt(elementSize);
            }

            //сохраняем формы
            changedForms.putAll(newForms);
            dataStream.writeInt(changedForms.size());
            for (FormDescriptor form : changedForms.values()) {
                int bytesWritten = outStream.size();
                form.serialize(dataStream);
                int formSize = outStream.size() - bytesWritten;
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

            changedForms.clear();
            newForms.clear();
            newElements.clear();
            FormDescriptor currentForm = formView.getForm();
            if (currentForm != null) {
                openForm(currentForm.ID);
            }
        } catch (IOException e) {
            throw new RuntimeException("Не могу сохранить форму.", e);
        }

        hasChangedNodes = false;

        setupActionButtons();

        updateUI();
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

    public void openForm(int ID) throws IOException {
        if (!changedForms.containsKey(ID)) {
            if (newForms.containsKey(ID)) {
                changedForms.put(ID, newForms.get(ID));
            } else {
                changedForms.put(ID, FormDescriptor.deserialize(visualNavigator.remoteNavigator.getRichDesignByteArray(ID),
                                                                visualNavigator.remoteNavigator.getFormEntityByteArray(ID)));
            }
        }

        FormDescriptor form = changedForms.get(ID);
        formView.setForm(form);

        form.removeDependency(captionUpdater);
        form.addDependency(form, "caption", captionUpdater);

        setupActionButtons();

        updateUI();
    }

    public void removeElement(int elementID) {
        FormDescriptor currentForm = formView.getForm();
        if (currentForm != null && currentForm.ID == elementID) {
            currentForm.getContext().setProperty(Lookup.DELETED_OBJECT_PROPERTY, currentForm);
        }
        changedForms.remove(elementID);
        newForms.remove(elementID);
        newElements.remove(elementID);
    }

    private void setupActionButtons() {
        previewBtn.setEnabled(formView.getForm() != null);
        saveBtn.setEnabled(!changedForms.isEmpty() || !newForms.isEmpty() || hasChangedNodes);
        cancelBtn.setEnabled(!changedForms.isEmpty() || !newForms.isEmpty() || hasChangedNodes);
    }

    public boolean isFormChanged(int formID) {
        return changedForms.containsKey(formID);
    }

    public FormDescriptor createAndOpenNewForm() {
        FormDescriptor newForm = new FormDescriptor(Main.generateNewID());

        newForms.put(newForm.ID, newForm);

        try {
            openForm(newForm.ID);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при открытии формы.", e);
        }

        return newForm;
    }

    public ClientNavigatorElement createNewNavigatorElement(String caption) {
        ClientNavigatorElement newElement = new ClientNavigatorElement(Main.generateNewID(), caption, false);

        newElements.put(newElement.ID, newElement);

        return newElement;
    }

    public void cancelForm(int formID) {
        FormDescriptor form = changedForms.get(formID);
        if (form != null) {
            if (newForms.containsKey(formID)) {
                form = new FormDescriptor(formID);
                newForms.put(formID, form);
            }

            changedForms.remove(formID);
            FormDescriptor currentForm = formView.getForm();
            if (currentForm != null && currentForm.ID == formID) {
                try {
                    openForm(formID);
                } catch (IOException e) {
                    throw new RuntimeException("Ошибка при отмене формы.", e);
                }
            }
        }
    }

    public void nodeChanged(NavigatorTreeNode node) {
        hasChangedNodes = true;
        setupActionButtons();
    }
}
