import core.search.nondeterministic.ResultsFunction;

import java.lang.reflect.Array;
import java.util.*;

public class VacuumFunctions {

    public static List<String> getActions1D(Object state){
        return Arrays.asList("suck", "right", "left");
    }

    public static List<String> getActions2D(Object state){
        return Arrays.asList("suck", "right", "left", "down", "up");
    }

    public static boolean testGoal1D(Object state){
        Set<String> s = (Set<String>) state;
        return !s.contains("dirty(0)") && !s.contains("dirty(1)") && s.contains("clean(0)") && s.contains("clean(1)");
    }

    public static boolean testGoal2D(Object state){
        Set<String> s = (Set<String>) state;
        return !s.contains("dirty(0,0)") && !s.contains("dirty(0,1)") && !s.contains("dirty(1,0)") && !s.contains("dirty(1,1)");
    }

    public static ResultsFunction<Set<String>, String> erraticResults() {
        return (Set<String> state, String action) -> {

            List<Set<String>> results = new ArrayList<>();
            Set<String> s = new HashSet<>(state);
            results.add(s);
            switch (action) {
                case "right" -> {
                    s.remove("pos(0)");
                    if (!s.contains("pos(1)"))
                        s.add("pos(1)");
                }
                case "left" -> {
                    s.remove("pos(1)");
                    if (!s.contains("pos(0)"))
                        s.add("pos(0)");
                }
                case "suck" -> {
                    if (s.contains("pos(0)") && s.contains("dirty(0)")) {
                        s.remove("dirty(0)");
                        s.add("clean(0)");
                        Set<String> s2 = new HashSet<>(s);
                        s2.remove("dirty(1)");
                        s2.add("clean(1)");
                        if (s != s2) {
                            results.add(s2);
                        }
                    } else if (s.contains("pos(1)") && s.contains("dirty(1)")) {
                        s.remove("dirty(1)");
                        s.add("clean(1)");
                        Set<String> s2 = new HashSet<>(s);
                        s2.remove("dirty(0)");
                        s2.add("clean(0)");
                        if (s != s2) {
                            results.add(s2);
                        }
                    } else if (s.contains("pos(0)") && !s.contains("dirty(0)")) {
                        Set<String> s2 = new HashSet<>(s);
                        s2.add("dirty(0)");
                        s2.remove("clean(0)");
                        if (s != s2) {
                            results.add(s2);
                        }
                    } else if (s.contains("pos(1)") && !s.contains("dirty(1)")) {
                        Set<String> s2 = new HashSet<>(s);
                        s2.add("dirty(1)");
                        s2.remove("clean(1)");
                        if (s != s2) {
                            results.add(s2);
                        }
                    }
                }
            }

            return results;
        };
    }

    public static ResultsFunction<Set<String>, String> slipperyResults() {
        return (Set<String> state, String action) -> {
            List<Set<String>> results = new ArrayList<>();
            Set<String> s = new HashSet<>(state);
            results.add(s);
            if(Objects.equals(action, "right")) {
                if(state.contains("pos(0)")){
                    Set<String> s2 = new HashSet<>(s);
                    s2.remove("pos(0)");
                    s2.add("pos(1)");
                    if (s != s2) {
                        results.add(s2);
                    }
                }
            } else if(Objects.equals(action, "left")) {
                if(state.contains("pos(1)")){
                    Set<String> s2 = new HashSet<>(s);
                    s2.remove("pos(1)");
                    s2.add("pos(0)");
                    if (s != s2) {
                        results.add(s2);
                    }
                }
            } else if(Objects.equals(action, "suck")) {
                if(state.contains("pos(0)")){
                    s.remove("dirty(0)");
                    s.add("clean(0)");
                } else if (state.contains("pos(1)")){
                    s.remove("dirty(1)");
                    s.add("clean(1)");
                }
            }
            return results;
        };
    }

    public static ResultsFunction<Set<String>, String> slipperyResults2D() {
        return (Set<String> state, String action) -> {
            List<Set<String>> results = new ArrayList<>();
            Set<String> s = new HashSet<>(state);
            results.add(s);
            if(Objects.equals(action, "right")) {
                if(state.contains("pos(1,0)") || state.contains("pos(0,0)")){
                    Set<String> s2 = new HashSet<>(s);
                    if(state.contains("pos(1,0)")){
                        s2.remove("pos(1,0)");
                        s2.add("pos(1,1)");
                    } else {
                        s2.remove("pos(0,0)");
                        s2.add("pos(0,1)");
                    }
                    if (s != s2) {
                        results.add(s2);
                    }
                }
            } else if(Objects.equals(action, "left")) {
                if(state.contains("pos(0,1)") || state.contains("pos(1,1)")){
                    Set<String> s2 = new HashSet<>(s);
                    if(state.contains("pos(0,1)")){
                        s2.remove("pos(0,1)");
                        s2.add("pos(0,0)");
                    } else {
                        s2.remove("pos(1,1)");
                        s2.add("pos(1,0)");
                    }
                    if (s != s2) {
                        results.add(s2);
                    }
                }
            } else if(Objects.equals(action, "down")) {
                if(state.contains("pos(0,1)") || state.contains("pos(0,0)")){
                    Set<String> s2 = new HashSet<>(s);
                    if(state.contains("pos(0,1)")){
                        s2.remove("pos(0,1)");
                        s2.add("pos(1,1)");
                    } else {
                        s2.remove("pos(0,0)");
                        s2.add("pos(1,0)");
                    }
                    if (s != s2) {
                        results.add(s2);
                    }
                }
            } else if(Objects.equals(action, "up")) {
                if(state.contains("pos(1,0)") || state.contains("pos(1,1)")){
                    Set<String> s2 = new HashSet<>(s);
                    if(state.contains("pos(1,0)")){
                        s2.remove("pos(1,0)");
                        s2.add("pos(0,0)");
                    } else {
                        s2.remove("pos(1,1)");
                        s2.add("pos(0,1)");
                    }
                    if (s != s2) {
                        results.add(s2);
                    }
                }
            }else if(Objects.equals(action, "suck")) {
                if(state.contains("pos(0,0)")){
                    s.remove("dirty(0,0)");
                } else if (state.contains("pos(0,1)")){
                    s.remove("dirty(0,1)");
                } else if (state.contains("pos(1,0)")){
                    s.remove("dirty(1,0)");
                } else if (state.contains("pos(1,1)")){
                    s.remove("dirty(1,1)");
                }
            }
            return results;
        };
    }
}
