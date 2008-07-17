/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.util.HashSet;
import java.util.Set;
import javax.swing.KeyStroke;

public class SwingUtils {
    
    static void addFocusTraversalKey(Component comp, int id, KeyStroke key) {
        
        Set keys = comp.getFocusTraversalKeys(id);
        Set newKeys = new HashSet(keys);
        newKeys.add(key);
        comp.setFocusTraversalKeys(id, newKeys);
    }

}
