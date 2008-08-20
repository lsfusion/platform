/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.util.HashSet;
import java.util.Set;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javax.swing.KeyStroke;
import javax.swing.tree.DefaultMutableTreeNode;

public class SwingUtils {
    
    static void addFocusTraversalKey(Component comp, int id, KeyStroke key) {
        
        Set keys = comp.getFocusTraversalKeys(id);
        Set newKeys = new HashSet(keys);
        newKeys.add(key);
        comp.setFocusTraversalKeys(id, newKeys);
    }

}

class ExpandingTreeNode extends DefaultMutableTreeNode {

    public String toString() { return "Retreiving data..."; }
}



  class FocusOwnerTracer implements PropertyChangeListener {

        public static final String FOCUS_OWNER_PROPERTY = "focusOwner";
		protected KeyboardFocusManager focusManager;

        protected static void installFocusTracer() {
            KeyboardFocusManager focusManager = KeyboardFocusManager.
                getCurrentKeyboardFocusManager();
            new FocusOwnerTracer(focusManager);
        }

        public FocusOwnerTracer(KeyboardFocusManager focusManager) {
			this.focusManager = focusManager;
			startListening();
		}

		public void startListening() {
			if (focusManager != null) {
                focusManager.addPropertyChangeListener(FOCUS_OWNER_PROPERTY, this);
			}
		}

		public void stopListening() {
			if (focusManager != null) {
                focusManager.removePropertyChangeListener(FOCUS_OWNER_PROPERTY, this);
			}
		}
		
		public void propertyChange(PropertyChangeEvent e) {
			Component oldOwner = (Component) e.getOldValue();
			Component newOwner = (Component) e.getNewValue();
			System.out.print("focusOwner changed: ");
			System.out.print(" old - " + oldOwner);
		    System.out.println(" new - " + newOwner);
		}

 }