package lsfusion.client.form.controller;

import bibliothek.gui.Dockable;
import bibliothek.gui.dock.action.actions.SimpleButtonAction;
import bibliothek.gui.dock.action.view.ActionViewConverter;
import bibliothek.gui.dock.action.view.ViewTarget;
import bibliothek.gui.dock.common.action.CAction;
import bibliothek.gui.dock.common.action.core.CommonDropDownItem;
import bibliothek.gui.dock.common.intern.action.CDropDownItem;
import lsfusion.client.base.view.ClientDockable;

import javax.swing.*;
import java.util.List;

import static lsfusion.client.ClientResourceBundle.getString;

public class CloseAllAction extends CDropDownItem<CloseAllAction.Action> {

    public CloseAllAction(List<ClientDockable> openedForms) {
        super(null);

        init(new Action(openedForms));
    }

    public class Action extends SimpleButtonAction implements CommonDropDownItem {

        public Action(List<ClientDockable> openedForms) {
            addActionListener(actionEvent -> {
                for (int i = openedForms.size() - 1; i >= 0; i--) {
                    openedForms.get(i).requestFocusInWindow();
                    openedForms.get(i).onClosing();
                }
            });
            setText(getString("form.close.all.tabs"));
            setIcon(new ImageIcon(this.getClass().getResource("/images/closeAllTabs.png")));
        }

        @Override
        public <V> V createView(ViewTarget<V> target, ActionViewConverter converter, Dockable dockable) {
            return target == ViewTarget.MENU ? super.createView(target, converter, dockable) : null;
        }

        public CAction getAction() {
            return CloseAllAction.this;
        }
    }
}