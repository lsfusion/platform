package lsfusion.client.form.object.table.tree;

import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.object.ClientObject;
import org.jdesktop.swingx.treetable.MutableTreeTableNode;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

// The desktop's tree model, as far as its nodes go: the server sends a tree level's rows only when they change, so a node
// of the level above that comes back, or moves under another node of its recursive level, gets the rows the level below
// last came with
public class GroupTreeTableModelTest {
    private final ClientTreeGroup treeGroup = new ClientTreeGroup();
    private final ClientGroupObject cat = group(60), item = group(61);
    private final ClientGroupObjectValue c1 = cat(1), c2 = cat(2), i15 = item(1, 5), i16 = item(1, 6), i27 = item(2, 7);
    private final GroupTreeTableModel model = new GroupTreeTableModel(false, "");

    public GroupTreeTableModelTest() {
        item.upTreeGroups.add(cat);
    }
    private ClientGroupObject group(int id) {
        ClientGroupObject group = new ClientGroupObject();
        group.ID = id;
        ClientObject object = new ClientObject();
        object.ID = id;
        object.groupObject = group;
        group.objects.add(object);
        group.parent = treeGroup;
        treeGroup.groups.add(group);
        return group;
    }
    private ClientGroupObjectValue cat(int category) {
        return new ClientGroupObjectValue(cat.objects.get(0), category);
    }
    private ClientGroupObjectValue item(int category, int item) {
        return new ClientGroupObjectValue(cat(category), new ClientGroupObjectValue(this.item.objects.get(0), item));
    }
    // the keys of a level and the parents the server sends beside them: none of a row of a level that does not recurse, nor
    // of a row at the top of one that does
    private void keys(ClientGroupObject group, List<ClientGroupObjectValue> keys, List<ClientGroupObjectValue> parents) {
        model.updateKeys(group, keys, parents, new HashMap<>());
    }
    private void keys(ClientGroupObject group, ClientGroupObjectValue... keys) {
        keys(group, Arrays.asList(keys), Collections.nCopies(keys.length, ClientGroupObjectValue.EMPTY));
    }
    private TreeGroupNode node(ClientGroupObject group, ClientGroupObjectValue key) {
        for (TreeGroupNode node : model.getGroupNodes(group))
            if (node.key.equals(key))
                return node;
        return null;
    }
    // a node's children: the keys of its rows, a "+" for the placeholder of the rows not read yet
    private static List<Object> children(TreeGroupNode node) {
        List<Object> children = new ArrayList<>();
        for (MutableTreeTableNode child : node.getChildren())
            children.add(child instanceof TreeGroupNode ? ((TreeGroupNode) child).key : "+");
        return children;
    }

    // a filter on the categories takes 1 away and gives it back: the items are the same all along, and not sent again
    @Test
    public void aNodeThatComesBackGetsTheRowsItsLevelBelowLastCameWith() {
        keys(cat, c1, c2);
        keys(item, i15, i16, i27);
        keys(cat, c2);
        keys(cat, c1, c2);
        assertEquals(Arrays.asList(i15, i16), children(node(cat, c1)));
        keys(item); // every node closed
        keys(cat, c2);
        keys(cat, c1, c2);
        assertEquals(Collections.singletonList("+"), children(node(cat, c1)));
    }

    // ... and so does a node that moves under another node of its group, the group recursive: the nodes of its own group
    // first, then the level below's
    @Test
    public void aNodeThatMovesKeepsItsRows() {
        cat.isRecursive = true;
        ClientGroupObjectValue c3 = cat(3), c4 = cat(4), i38 = item(3, 8), top = ClientGroupObjectValue.EMPTY;
        keys(cat, Arrays.asList(c1, c2, c3, c4), Arrays.asList(top, top, c1, c3)); // 3 under 1, 4 under 3
        keys(item, i27, i38);
        keys(cat, Arrays.asList(c1, c2, c3, c4), Arrays.asList(top, top, c2, c3)); // 3 moves under 2
        assertEquals(Arrays.asList(c3, i27), children(node(cat, c2)));
        assertEquals(Arrays.asList(c4, i38), children(node(cat, c3)));
    }
}
