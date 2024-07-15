import core.search.nondeterministic.ResultsFunction;

import java.util.*;

public class TestFunction {

    public static final Set<String> initTest1 = Set.of("s0");

    public static List<String> getActionsTest1(Object state){
        return Arrays.asList("a1", "a2", "a3");
    }

    public static boolean testGoalTest1(Object state){
        Set<String> s = (Set<String>) state;
        return s.contains("goal");
    }

    public static ResultsFunction<Set<String>, String> test1Results() {
        return (Set<String> state, String action) -> {

            List<Set<String>> results = new ArrayList<>();
            Set<String> s = new HashSet<>(state);
            results.add(s);
            switch (action) {
                case "a1" -> {
                    if(state.contains("s0")){
                        s.remove("s0");
                        s.add("s1");
                        Set<String> s2 = new HashSet<>();
                        s2.add("s0");
                        results.add(s2);
                    }
                }
                case "a2" -> {
                    if(state.contains("s1")){
                        s.remove("s1");
                        s.add("s2");
                        Set<String> s2 = new HashSet<>();
                        s2.add("s0");
                        results.add(s2);
                    }
                }
                case "a3" -> {
                    if(state.contains("s2")){
                        s.remove("s2");
                        s.add("goal");
                    }
                }
            }

            return results;
        };
    }

}
