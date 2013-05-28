package platform.client.descriptor.editor.base;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ActionMapUIResource;
import java.awt.event.*;

/**
 * Maintenance tip - There were some tricks to getting this code
 * working:
 *
 * 1. You have to overwite addMouseListener() to do nothing
 * 2. You have to add a mouse event on mousePressed by calling
 * super.addMouseListener()
 * 3. You have to replace the UIActionMap for the keyboard event
 * "pressed" with your own one.
 * 4. You have to remove the UIActionMap for the keyboard event
 * "released".
 * 5. You have to grab focus when the next state is entered,
 * otherwise clicking on the component won't get the focus.
 * 6. You have to make a TristateDecorator as a button model that
 * wraps the original button model and does state management.
 */
public abstract class AbstractTristateCheckBox extends JCheckBox {
    /** This is a type-safe enumerated type */
  private final TristateDecorator model;

  // требование к изменению (но сама не изменяет)
  protected abstract void onChange();

  public AbstractTristateCheckBox(String text, Icon icon, Tristate initial){
    super(text, icon);
    // Add a listener for when the mouse is pressed
    super.addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        grabFocus();
        onChange();
      }
    });
    // Reset the keyboard action map
    ActionMap map = new ActionMapUIResource();
    map.put("pressed", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        grabFocus();
        onChange();
      }
    });
    map.put("released", null);
    SwingUtilities.replaceUIActionMap(this, map);
    // set the model to the adapted model
    model = new TristateDecorator(getModel());
    setModel(model);
    setState(initial);
  }
  public AbstractTristateCheckBox(String text, Tristate initial) {
    this(text, null, initial);
  }
  public AbstractTristateCheckBox(String text) {
    this(text, Tristate.MIXED);
  }
  public AbstractTristateCheckBox() {
    this(null);
  }

  /** No one may add mouse listeners, not even Swing! */
  public void addMouseListener(MouseListener l) { }
  /**
   * Set the new state to either SELECTED, NOT_SELECTED or
   * DONT_CARE.  If state == null, it is treated as DONT_CARE.
   */
  public void setState(Tristate state) { model.setState(state); }
  /** Return the current state, which is determined by the
   * selection status of the model. */
  public Tristate getState() { return model.getState(); }
  public void setSelected(boolean b) {
    if (b) {
      setState(Tristate.SELECTED);
    } else {
      setState(Tristate.NOT_SELECTED);
    }
  }


  /**
   * Exactly which Design Pattern is this?  Is it an Adapter,
   * a Proxy or a Decorator?  In this case, my vote lies with the
   * Decorator, because we are extending functionality and
   * "decorating" the original model with a more powerful model.
   */
  private class TristateDecorator implements ButtonModel {
    private final ButtonModel other;
    private TristateDecorator(ButtonModel other) {
      this.other = other;
    }
    private void setState(Tristate state) {
      switch(state) {
        case NOT_SELECTED:
            other.setArmed(false);
            other.setPressed(false);
            other.setSelected(false);
            break;
        case SELECTED:
            other.setArmed(false);
            other.setPressed(false);
            other.setSelected(true);
            break;
        case MIXED:
            other.setArmed(true);
            other.setPressed(true);
            other.setSelected(true);
      }
    }
    /**
     * The current state is embedded in the selection / armed
     * state of the model.
     *
     * We return the SELECTED state when the checkbox is selected
     * but not armed, DONT_CARE state when the checkbox is
     * selected and armed (grey) and NOT_SELECTED when the
     * checkbox is deselected.
     */
    private Tristate getState() {
      if (isSelected() && !isArmed()) {
        // normal black tick
        return Tristate.SELECTED;
      } else if (isSelected() && isArmed()) {
        // don't care grey tick
        return Tristate.MIXED;
      } else {
        // normal deselected
        return Tristate.NOT_SELECTED;
      }
    }
    /** Filter: No one may change these statuses except us. */
    public void setArmed(boolean b) { }
    public void setSelected(boolean b) { }
    public void setPressed(boolean b) { }

    /** We disable focusing on the component when it is not
     * enabled. */
    public void setEnabled(boolean b) {
      setFocusable(b);
      other.setEnabled(b);
    }
    /** All these methods simply delegate to the "other" model
     * that is being decorated. */
    public boolean isArmed() { return other.isArmed(); }
    public boolean isSelected() { return other.isSelected(); }
    public boolean isEnabled() { return other.isEnabled(); }
    public boolean isPressed() { return other.isPressed(); }
    public boolean isRollover() { return other.isRollover(); }
    public void setRollover(boolean b) { other.setRollover(b); }
    public void setMnemonic(int key) { other.setMnemonic(key); }
    public int getMnemonic() { return other.getMnemonic(); }
    public void setActionCommand(String s) {
      other.setActionCommand(s);
    }
    public String getActionCommand() {
      return other.getActionCommand();
    }
    public void setGroup(ButtonGroup group) {
      other.setGroup(group);
    }
    public void addActionListener(ActionListener l) {
      other.addActionListener(l);
    }
    public void removeActionListener(ActionListener l) {
      other.removeActionListener(l);
    }
    public void addItemListener(ItemListener l) {
      other.addItemListener(l);
    }
    public void removeItemListener(ItemListener l) {
      other.removeItemListener(l);
    }
    public void addChangeListener(ChangeListener l) {
      other.addChangeListener(l);
    }
    public void removeChangeListener(ChangeListener l) {
      other.removeChangeListener(l);
    }
    public Object[] getSelectedObjects() {
      return other.getSelectedObjects();
    }
  }
}
