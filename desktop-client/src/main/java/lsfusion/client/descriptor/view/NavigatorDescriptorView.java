package lsfusion.client.descriptor.view;

import lsfusion.base.context.IncrementView;
import lsfusion.base.context.Lookup;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.descriptor.FormDescriptor;
import lsfusion.client.navigator.ClientNavigator;
import lsfusion.client.navigator.ClientNavigatorElement;
import lsfusion.client.navigator.NavigatorTreeNode;
import lsfusion.client.tree.ClientTreeNode;
import lsfusion.interop.navigator.RemoteNavigatorInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class NavigatorDescriptorView extends JPanel {
    private final RemoteNavigatorInterface remoteNavigator;

    private final FormDescriptorView formView;
    private final VisualSetupNavigatorPanel visualNavigator;

    private final Map<String, FormDescriptor> newForms = new HashMap<String, FormDescriptor>();
    private final Map<String, ClientNavigatorElement> newElements = new HashMap<String, ClientNavigatorElement>();
    private final Map<String, FormDescriptor> changedForms = new HashMap<String, FormDescriptor>();

    private final JButton previewBtn;
    private final JButton generateCodeBtn;

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

        generateCodeBtn = new JButton(ClientResourceBundle.getString("descriptor.view.generate.code"));
        generateCodeBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                GenerateCodeDialog dlg = new GenerateCodeDialog(formView.getForm());
                dlg.setBounds(SwingUtilities.windowForComponent(NavigatorDescriptorView.this).getBounds());
                dlg.setVisible(true);
            }
        });

        JPanel commandPanel = new JPanel();
        commandPanel.setLayout(new BoxLayout(commandPanel, BoxLayout.X_AXIS));
        commandPanel.add(Box.createRigidArea(new Dimension(5, 5)));
        commandPanel.add(previewBtn);
        commandPanel.add(generateCodeBtn);
        commandPanel.add(Box.createRigidArea(new Dimension(20, 5)));
        commandPanel.add(Box.createHorizontalGlue());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(visualNavigator), formView);
        splitPane.setResizeWeight(0.1);

        add(splitPane, BorderLayout.CENTER);
        add(commandPanel, BorderLayout.SOUTH);

        try {
//            String formSid = clientNavigator.remoteNavigator.getCurrentFormSID();
            String formSid = null;//todo: get current form SID from docking frames
            if (formSid != null) {
                openForm(formSid);
                formView.openActiveGroupObject();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        previewBtn.setEnabled(formView.getForm() != null);
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
        setupActionButtons();
    }
}
