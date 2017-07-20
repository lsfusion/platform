package lsfusion.client;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.ListIterator;


public class FormFocusTraversalPolicy extends LayoutFocusTraversalPolicy {
    private ArrayList<Component> list;

    public FormFocusTraversalPolicy() {
        list = new ArrayList<>();
        setComparator(new LayoutComparator());
    }

    @Override
    public Component getDefaultComponent(Container aContainer) {
        if (list.size() == 0) {
            return super.getDefaultComponent(aContainer);
        }
        Component c = list.get(0);
        if (c instanceof Container) {
            return super.getDefaultComponent((Container) c);
        } else {
            return c;
        }
    }

    public void addDefault(Component c) {
        list.add(c);
    }

    public void removeDefault(Component c) {
        list.remove(c);
    }

    //c/p from javax.swing.LayoutComparator, чтобы можно было указать вертикальный траверсинг
    private final class LayoutComparator implements Comparator {

        private static final int ROW_TOLERANCE = 10;

        private boolean horizontal = false;
        private boolean leftToRight = true;

        public int compare(Object o1, Object o2) {
            Component a = (Component) o1;
            Component b = (Component) o2;

            if (a == b) {
                return 0;
            }

            // Row/Column algorithm only applies to siblings. If 'a' and 'b'
            // aren't siblings, then we need to find their most inferior
            // ancestors which share a parent. Compute the ancestory lists for
            // each Component and then search from the Window down until the
            // hierarchy branches.
            if (a.getParent() != b.getParent()) {
                LinkedList aAncestory, bAncestory;

                for (aAncestory = new LinkedList(); a != null; a = a.getParent()) {
                    aAncestory.add(a);
                    if (a instanceof Window) {
                        break;
                    }
                }
                if (a == null) {
                    // 'a' is not part of a Window hierarchy. Can't cope.
                    throw new ClassCastException();
                }

                for (bAncestory = new LinkedList(); b != null; b = b.getParent()) {
                    bAncestory.add(b);
                    if (b instanceof Window) {
                        break;
                    }
                }
                if (b == null) {
                    // 'b' is not part of a Window hierarchy. Can't cope.
                    throw new ClassCastException();
                }

                for (ListIterator
                             aIter = aAncestory.listIterator(aAncestory.size()),
                             bIter = bAncestory.listIterator(bAncestory.size()); ; ) {
                    if (aIter.hasPrevious()) {
                        a = (Component) aIter.previous();
                    } else {
                        // a is an ancestor of b
                        return -1;
                    }

                    if (bIter.hasPrevious()) {
                        b = (Component) bIter.previous();
                    } else {
                        // b is an ancestor of a
                        return 1;
                    }

                    if (a != b) {
                        break;
                    }
                }
            }

            int ax = a.getX(), ay = a.getY(), bx = b.getX(), by = b.getY();

            int zOrder = a.getParent().getComponentZOrder(a) - b.getParent().getComponentZOrder(b);
            if (horizontal) {
                if (leftToRight) {

                    // LT - Western Europe (optional for Japanese, Chinese, Korean)

                    if (Math.abs(ay - by) < ROW_TOLERANCE) {
                        return (ax < bx) ? -1 : ((ax > bx) ? 1 : zOrder);
                    } else {
                        return (ay < by) ? -1 : 1;
                    }
                } else { // !leftToRight

                    // RT - Middle East (Arabic, Hebrew)

                    if (Math.abs(ay - by) < ROW_TOLERANCE) {
                        return (ax > bx) ? -1 : ((ax < bx) ? 1 : zOrder);
                    } else {
                        return (ay < by) ? -1 : 1;
                    }
                }
            } else { // !horizontal
                if (leftToRight) {

                    // TL - Mongolian

                    if (Math.abs(ax - bx) < ROW_TOLERANCE) {
                        return (ay < by) ? -1 : ((ay > by) ? 1 : zOrder);
                    } else {
                        return (ax < bx) ? -1 : 1;
                    }
                } else { // !leftToRight

                    // TR - Japanese, Chinese, Korean

                    if (Math.abs(ax - bx) < ROW_TOLERANCE) {
                        return (ay < by) ? -1 : ((ay > by) ? 1 : zOrder);
                    } else {
                        return (ax > bx) ? -1 : 1;
                    }
                }
            }
        }
    }
}
