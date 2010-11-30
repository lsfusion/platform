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

    private final IncrementView updateHandler = new IncrementView() {
        public void update(Object updateObject, String updateField) {
            if (formView.getUpdated()) {
                FormDescriptor currentForm = formView.getForm();
                if (currentForm != null) {
                    changedForms.put(currentForm.getID(), currentForm);
                    visualNavigator.getTree().updateUI();
                    setupActionButtons();
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

            formView.setUpdated(false);
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
            Map<String, List<String>> changedElements = getChangedNavigatorElementsChildren();
            dataStream.writeInt(changedElements.size());
            for (Map.Entry<String, List<String>> entry : changedElements.entrySet()) {
                dataStream.writeUTF(entry.getKey());
                dataStream.writeInt(entry.getValue().size());
                for (String childSID : entry.getValue()) {
                    dataStream.writeUTF(childSID);
                }
            }

            visualNavigator.remoteNavigator.saveVisualSetup(outStream.toByteArray());

            formView.setUpdated(false);
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
    }

    private Map<String, List<String>> getChangedNavigatorElementsChildren() {
        HashMap<String, List<String>> result = new HashMap<String, List<String>>();
        Enumeration<ClientTreeNode> nodes = visualNavigator.getTree().rootNode.depthFirstEnumeration();
        while (nodes.hasMoreElements()) {
            ClientTreeNode node = nodes.nextElement();
            if (node instanceof NavigatorTreeNode) {
                NavigatorTreeNode navigatorNode = (NavigatorTreeNode) node;
                if (navigatorNode.nodeStructureChanged) {
                    navigatorNode.nodeStructureChanged = false;
                    List<String> children = new ArrayList<String>();
                    for (int i = 0; i < navigatorNode.getChildCount(); ++i) {
                        ClientTreeNode childNode = (ClientTreeNode) navigatorNode.getChildAt(i);
                        if (childNode instanceof NavigatorTreeNode) {
                            NavigatorTreeNode childNavigatorNode = (NavigatorTreeNode) childNode;
                            children.add(childNavigatorNode.navigatorElement.sID);
                        }
                    }
                    result.put(navigatorNode.navigatorElement.sID, children);
                }
            }
        }

        return result;
    }

    public void openForm(int ID) throws IOException {
        FormDescriptor form = changedForms.get(ID);
        if (form == null) {
            if (newForms.containsKey(ID)) {
                form = newForms.get(ID);
            } else {
                form = FormDescriptor.deserialize(visualNavigator.remoteNavigator.getRichDesignByteArray(ID),
                                                  visualNavigator.remoteNavigator.getFormEntityByteArray(ID));
            }
        }

        formView.setForm(form);

        form.removeDependency(captionUpdater);
        form.addDependency(form, "caption", captionUpdater);
        form.addDependency(form, "updated", updateHandler);

        setupActionButtons();
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
        boolean hasChanges = !changedForms.isEmpty() || !newForms.isEmpty() || !newElements.isEmpty() || hasChangedNodes;

        previewBtn.setEnabled(formView.getForm() != null);
        saveBtn.setEnabled(hasChanges);
        cancelBtn.setEnabled(hasChanges);

        updateUI();
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
