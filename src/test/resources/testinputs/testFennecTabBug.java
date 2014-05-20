package testinputs;

import testutils.BaseIntegrationTestCase;

import java.util.LinkedList;

public class testFennecTabBug extends BaseIntegrationTestCase {
    LinkedList<Tab> mOrder = new LinkedList<Tab>();

    private Tab getNextTabFrom(Tab tab, boolean getPrivate) {
        int numTabs = mOrder.size();
        int index = getIndexOf(tab);
        for (int i = index + 1; i < numTabs; i++) {
            Tab next = mOrder.get(i);
            if (next.isPrivate() == getPrivate) {
                return next;
            }
        }
        return null;
    }

    private int getIndexOf(Tab tab) {
        return 0;
    }

    private static class Tab {
        public boolean isPrivate() {
            return false;
        }
    }

    @Override
    protected void test() {
        getNextTabFrom(new Tab(), false);
        print("IT'S TOASTER TIME!");
    }
}
