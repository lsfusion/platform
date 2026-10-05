package lsfusion.gwt.client.form.object.table.tree.view;

import com.google.gwt.junit.client.GWTTestCase;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.base.jsni.JsniTestSupport;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.classes.data.GIntegerType;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.GObject;
import lsfusion.gwt.client.form.object.table.tree.GTreeGroup;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

// The platform's own tree, as far as its model goes: the nodes built from the keys a tree's groups get, and what the
// tree shows of a node asked to open or close until the server answers - GTreeTable.expandNode sets it on the model, and
// GTreeTableTree.synchronize leaves it to a stale answer and drops it on the request's own
public class GTreeTableTreeTest extends GWTTestCase {
    @Override public String getModuleName() { return "lsfusion.gwt.main"; }

    private final GTreeGroup treeGroup = new GTreeGroup();
    private final GGroupObject cat = new GGroupObject(), item = new GGroupObject();
    private final GGroupObjectValue c1 = new GGroupObjectValue(60, 1), c2 = new GGroupObjectValue(60, 2);
    private final GGroupObjectValue i15 = itemKey(1, 5), i16 = itemKey(1, 6), i27 = itemKey(2, 7);
    private GTreeTableTree tree;

    @Override protected void gwtSetUp() {
        JsniTestSupport.installMapDelete();
        group(cat, 60, "cat");
        group(item, 61, "item");
        item.upTreeGroups.add(cat);
        tree = new GTreeTableTree(new GForm());
    }
    private void group(GGroupObject group, int id, String sid) {
        group.ID = id; group.nativeSID = "g" + id; group.sID = sid;
        group.parent = treeGroup;
        treeGroup.groups.add(group);
        group.objects.add(new GObject(group, sid, id, sid, GIntegerType.instance));
    }
    private static GGroupObjectValue itemKey(int category, int item) {
        return new GGroupObjectValue(2, new int[]{60, 61}, new Serializable[]{category, item});
    }
    private static ArrayList<GGroupObjectValue> keys(GGroupObjectValue... keys) { return new ArrayList<>(Arrays.asList(keys)); }
    // the parents the server sends beside a group's keys: none of a row of a group that does not recurse
    private static ArrayList<GGroupObjectValue> noParents(int count) {
        ArrayList<GGroupObjectValue> parents = new ArrayList<>();
        for (int i = 0; i < count; i++)
            parents.add(GGroupObjectValue.EMPTY);
        return parents;
    }
    private void categories(int requestIndex) {
        NativeHashMap<GGroupObjectValue, Integer> counts = new NativeHashMap<>();
        counts.put(c1, 2);
        counts.put(c2, 1);
        tree.setKeys(cat, keys(c1, c2), noParents(2), counts, requestIndex);
    }
    private void items(int requestIndex, GGroupObjectValue... keys) {
        tree.setKeys(item, keys(keys), noParents(keys.length), new NativeHashMap<>(), requestIndex);
    }
    private GTreeObjectTableNode node(GGroupObject group, GGroupObjectValue key) {
        return tree.getGroupNodes(group).get(key);
    }
    // what a node shows, as its expander glyph does: open while it has children in the model - a placeholder's too
    private static String children(GTreeContainerTableNode node) {
        StringBuilder result = new StringBuilder();
        for (GTreeChildTableNode child : node.getChildren())
            result.append(child instanceof GTreeObjectTableNode ? ((GTreeObjectTableNode) child).getKey().toKeyString() : "?").append(' ');
        return result.toString().trim();
    }
    // ... as the keys would say it: a placeholder is "?"
    private static String expected(Object... children) {
        StringBuilder result = new StringBuilder();
        for (Object child : children)
            result.append(child instanceof GGroupObjectValue ? ((GGroupObjectValue) child).toKeyString() : "?").append(' ');
        return result.toString().trim();
    }
    // a node asked to open or close, as GTreeTable.expandNode leaves it in the model: opened, it shows a placeholder for
    // each child it is said to have; closed, its children are taken away at once
    private void ask(GTreeObjectTableNode node, boolean open, long requestIndex) {
        node.setPendingExpanding(open, requestIndex);
        if (open) {
            for (int i = 0; i < node.getExpandableChildren(); i++)
                node.addNode(i, new GTreeExpandingTableNode(i));
        } else {
            tree.removeChildrenFromGroupNodes(node);
            node.setChildren(new ArrayList<>());
        }
    }

    public void testTheKeysBuildTheNodes() {
        categories(0);
        items(0, i15, i16);
        assertEquals(expected(c1, c2), children(tree.root));
        assertEquals(expected(i15, i16), children(node(cat, c1)));
        assertEquals(expected(), children(node(cat, c2)));
        assertTrue(node(cat, c2).isExpandable()); // counted, not loaded
        assertFalse(node(item, i15).isExpandable()); // the bottom group has no children at all
    }
    public void testANodeAskedOpenIsLeftAsAskedByAStaleAnswerAndOpenedByItsOwn() {
        categories(0);
        items(0, i15, i16);
        ask(node(cat, c2), true, 2);
        assertEquals(expected("?"), children(node(cat, c2))); // a placeholder for its one child, at once
        items(1, i15, i16); // an answer to an earlier request: 2 is left as asked
        assertEquals(expected("?"), children(node(cat, c2)));
        assertNotNull(node(cat, c2).pendingExpanding);
        items(2, i15, i16, i27); // its own answer: the child itself
        assertEquals(expected(i27), children(node(cat, c2)));
        assertNull(node(cat, c2).pendingExpanding);
        assertEquals(expected(i15, i16), children(node(cat, c1))); // the other node, never asked, follows every answer
    }
    public void testANodeAskedClosedIsLeftAsAskedByAStaleAnswerAndClosedByItsOwn() {
        categories(0);
        items(0, i15, i16, i27);
        ask(node(cat, c2), false, 3);
        assertEquals(expected(), children(node(cat, c2))); // its children go at once
        assertNull(node(item, i27));
        items(2, i15, i16, i27); // an answer to an earlier request still has the child: 2 is left as asked
        assertEquals(expected(), children(node(cat, c2)));
        items(3, i15, i16); // its own answer
        assertEquals(expected(), children(node(cat, c2)));
        assertNull(node(cat, c2).pendingExpanding);
        items(4, i15, i16, i27); // ... and a later answer that opens it again is followed
        assertEquals(expected(i27), children(node(cat, c2)));
    }
}
