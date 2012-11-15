package platform.client.descriptor.view;

import platform.base.context.IncrementView;
import platform.base.context.Lookup;
import platform.client.ClientResourceBundle;
import platform.client.Main;
import platform.client.descriptor.FormDescriptor;
import platform.client.navigator.ClientNavigator;
import platform.client.navigator.ClientNavigatorElement;
import platform.client.navigator.NavigatorTreeNode;
import platform.client.remote.proxy.RemoteFormProxy;
import platform.client.tree.ClientTreeNode;
import platform.interop.navigator.RemoteNavigatorInterface;

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
    private final RemoteNavigatorInterface remoteNavigator;

    private final FormDescriptorView formView;
    private final VisualSetupNavigatorPanel visualNavigator;

    private final Map<String, FormDescriptor> newForms = new HashMap<String, FormDescriptor>();
    private final Map<String, ClientNavigatorElement> newElements = new HashMap<String, ClientNavigatorElement>();
    private final Map<String, FormDescriptor> changedForms = new HashMap<String, FormDescriptor>();

    private final JButton previewBtn;
    private final JButton saveBtn;
    private final JButton cancelBtn;

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
                            updateTree();
                            break;
                        }
                    }
                }
            }
        }
    };

    private void updateTree() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                visualNavigator.getTree().updateUI();
            }
        });
    }

    private final IncrementView updateHandler = new IncrementView() {
        public void update(Object updateObject, String updateField) {
            if (formView.getUpdated()) {
                FormDescriptor currentForm = formView.getForm();
                if (currentForm != null) {
                    changedForms.put(currentForm.getSID(), currentForm);
                    updateTree();
                    setupActionButtons();
                }
            }
        }
    };

    public NavigatorDescriptorView(final ClientNavigator clientNavigator) {
        setLayout(new BorderLayout());

        remoteNavigator = clientNavigator.remoteNavigator;
        visualNavigator = new VisualSetupNavigatorPanel(this, clientNavigator);

        formView = new FormDescriptorView();

        previewBtn = new JButton(ClientResourceBundle.getString("descriptor.view.form.preview"));
        previewBtn.setEnabled(false);
        previewBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                PreviewDialog dlg = new PreviewDialog(clientNavigator, formView.getForm());
                dlg.setBounds(SwingUtilities.windowForComponent(NavigatorDescriptorView.this).getBounds());
                dlg.setVisible(true);
            }
        });

        saveBtn = new JButton(ClientResourceBundle.getString("descriptor.view.save.changes"));
        saveBtn.setEnabled(false);
        saveBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                commitChanges();
            }
        });

        cancelBtn = new JButton(ClientResourceBundle.getString("descriptor.view.undo.changes"));
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

        try {
            String formSid = clientNavigator.remoteNavigator.getCurrentFormSID();
            if (formSid != null) {
                openForm(formSid);
                formView.openActiveGroupObject();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cancelChanges() {
        try {
            visualNavigator.cancelNavigatorChanges();

            while (newForms.size() > 0) {
                FormDescriptor form = newForms.values().iterator().next();
                removeElement(form.getSID());
                newForms.remove(form.getSID());
            }

            formView.setUpdated(false);
            changedForms.clear();

            FormDescriptor currentForm = formView.getForm();
            if (currentForm != null) {
                openForm(currentForm.getSID());
            }
        } catch (IOException e) {
            throw new RuntimeException(ClientResourceBundle.getString("descriptor.view.can.not.open.form"), e);
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

            remoteNavigator.saveVisualSetup(outStream.toByteArray());

            formView.setUpdated(false);
            changedForms.clear();
            newForms.clear();
            newElements.clear();
            FormDescriptor currentForm = formView.getForm();
            if (currentForm != null) {
                openForm(currentForm.getSID());
            }

            // нужно обязательно сбрасывать кэши, иначе при попытке открыть форму в навигаторе
            // будет закэширована старая форма и пойдет Exception при применении изменений
            RemoteFormProxy.dropCaches();
        } catch (IOException e) {
            throw new RuntimeException(ClientResourceBundle.getString("descriptor.view.can.not.save.form"), e);
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

    public void openForm(String sID) throws IOException {
        FormDescriptor form = changedForms.get(sID);
        if (form == null) {
            if (newForms.containsKey(sID)) {
                form = newForms.get(sID);
            } else {
                form = FormDescriptor.deserialize(remoteNavigator.getRichDesignByteArray(sID),
                                                  remoteNavigator.getFormEntityByteArray(sID));
            }
        }

        formView.setForm(form);

        form.removeDependency(captionUpdater);
        form.addDependency(form, "caption", captionUpdater);
        form.addDependency(form, "updated", updateHandler);

        setupActionButtons();
    }

    public void removeElement(String elementSID) {
        FormDescriptor currentForm = formView.getForm();
        if (currentForm != null && currentForm.getSID().equals(elementSID)) {
            currentForm.getContext().setProperty(Lookup.DELETED_OBJECT_PROPERTY, currentForm);
        }
        changedForms.remove(elementSID);
        newForms.remove(elementSID);
        newElements.remove(elementSID);
    }

    private void setupActionButtons() {
        boolean hasChanges = !changedForms.isEmpty() || !newForms.isEmpty() || !newElements.isEmpty() || hasChangedNodes;

        previewBtn.setEnabled(formView.getForm() != null);
        //todo: включить кнопку, если когда-нибудь вообще этот механизм будет нужен, т.к. сейчас он фактически не работает...
//        saveBtn.setEnabled(hasChanges);
        cancelBtn.setEnabled(hasChanges);

        updateUI();
    }

    public boolean isFormChanged(String formSID) {
        return changedForms.containsKey(formSID);
    }

    public FormDescriptor createAndOpenNewForm() {
        FormDescriptor newForm = new FormDescriptor(null);

        newForms.put(newForm.getSID(), newForm);

        try {
            openForm(newForm.getSID());
        } catch (IOException e) {
            throw new RuntimeException(ClientResourceBundle.getString("errors.error.opening.form"), e);
        }

        return newForm;
    }

    public ClientNavigatorElement createNewNavigatorElement(String caption) {
        ClientNavigatorElement newElement = new ClientNavigatorElement(Main.generateNewID(), caption, caption, false);

        newElements.put(newElement.getSID(), newElement);

        return newElement;
    }

    public void cancelForm(String formSID) {
        FormDescriptor form = changedForms.get(formSID);
        if (form != null) {
            if (newForms.containsKey(formSID)) {
                form = new FormDescriptor(formSID);
                newForms.put(formSID, form);
            }

            changedForms.remove(formSID);
            FormDescriptor currentForm = formView.getForm();
            if (currentForm != null && currentForm.getSID().equals(formSID)) {
                try {
                    openForm(formSID);
                } catch (IOException e) {
                    throw new RuntimeException(ClientResourceBundle.getString("descriptor.view.error.cancelling.form"), e);
                }
            }
        }
    }

    public void nodeChanged(NavigatorTreeNode node) {
        hasChangedNodes = true;
        setupActionButtons();
    }
}
