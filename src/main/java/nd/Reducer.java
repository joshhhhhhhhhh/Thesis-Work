package nd;

import jason.asSyntax.*;
import jason.asSyntax.parser.ParseException;

import java.util.*;

public class Reducer {

    public static void reduce(PlanLibrary library) throws ParseException {
        /*
        Map<Term, List<Term>> plansSortedByAction = new HashMap<>();
        for (Plan plan: library.getPlans()) {
            Term t = plan.getBody().getBodyTerm();
            if(!plansSortedByAction.containsKey(t))
                plansSortedByAction.put(t, new ArrayList<>());
            plansSortedByAction.get(t).add(plan.getContext());
        }

        for (List<Term> context: plansSortedByAction.values()) {
            System.out.println(context);
        }*/

        Map<String, Plan> bodyStrings = new HashMap<>();
        for (Plan plan: library.clone().getPlans()) {
            String originalPlanBody = plan.getBody().toString();
            if(!bodyStrings.containsKey(originalPlanBody))
                bodyStrings.put(originalPlanBody, plan);
            else {
                String originalContext;
                if(bodyStrings.get(originalPlanBody).getContext() == null){
                    originalContext = "_";
                } else {
                    originalContext = bodyStrings.get(originalPlanBody).getContext().toString();
                }
                String newContext;
                if(plan.getContext() == null){
                    newContext = "_";
                } else {
                    newContext = plan.getContext().toString();
                }
                String updatedContext = "(" + newContext + ") | (" + originalContext + ")";
                library.get(bodyStrings.get(originalPlanBody).getLabel()).setContext(ASSyntax.parseFormula(updatedContext));
                bodyStrings.put(originalPlanBody, library.get(bodyStrings.get(originalPlanBody).getLabel()));
                library.remove(plan.getLabel());
            }
        }
    }
}
