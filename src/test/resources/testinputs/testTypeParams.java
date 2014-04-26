package testinputs;

import testutils.BaseIntegrationTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class testTypeParams extends BaseIntegrationTestCase {
    public <T extends Collection<Q>, Q> List<Q> merge(List<T> collections) {
        List<Q> combined = new ArrayList<Q>();
        for (T c : collections) {
            combined.addAll(c);
        }

        return combined;
    }

    @Override
    protected void test() {
        List<ArrayList<Integer>> inputs = new ArrayList<ArrayList<Integer>>();

        ArrayList<Integer> a = new ArrayList<Integer>();
        a.add(1);
        a.add(2);
        a.add(3);
        inputs.add(a);
        ArrayList<Integer> b = new ArrayList<Integer>();
        a.add(4);
        a.add(5);
        inputs.add(b);
        ArrayList<Integer> c = new ArrayList<Integer>();
        a.add(6);
        inputs.add(c);

        List<Integer> someInts = merge(inputs);
        print(Arrays.toString(someInts.toArray()));
    }
}
