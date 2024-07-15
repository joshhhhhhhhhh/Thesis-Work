import jason.asSyntax.Plan;
import jason.asSyntax.PlanBody;
import jason.asSyntax.PlanLibrary;
import jason.asSyntax.Term;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Reducer {

    public static void reduce(PlanLibrary library) {
        Map<Term, List<Term>> plansSortedByAction = new HashMap<>();
        for (Plan plan: library.getPlans()) {
            Term t = plan.getBody().getBodyTerm();
            if(!plansSortedByAction.containsKey(t))
                plansSortedByAction.put(t, new ArrayList<>());
            plansSortedByAction.get(t).add(plan.getContext());
        }

        for (List<Term> context: plansSortedByAction.values()) {
            System.out.println(context);
        }

    }
}
