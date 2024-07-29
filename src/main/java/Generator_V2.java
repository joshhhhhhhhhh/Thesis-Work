import core.search.nondeterministic.Plan;
import jason.JasonException;
import jason.asSemantics.TransitionSystem;
import jason.asSyntax.*;
import jason.asSyntax.parser.ParseException;

import java.util.Random;
import java.util.Set;

public class Generator_V2 {


    public Generator_V2(){}

    public PlanLibrary generate(Plan<Set<Literal>, Literal> root, Set<Literal> initialState){
        try{
            return recursiveGenerate(root, initialState, new PlanLibrary(), null);
        } catch (JasonException | ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public PlanLibrary recursiveGenerate(Plan<Set<Literal>, Literal> plan, Set<Literal> state, PlanLibrary planLibrary, Pred parentLabel) throws JasonException, ParseException {
        if(plan.isEmpty())
            return planLibrary;

        Trigger trigger = new Trigger(Trigger.TEOperator.add, Trigger.TEType.achieve, Literal.parseLiteral("act"));

        //ListTermImpl context = new ListTermImpl();
        //context.addAll(state);
        String contextString = "";
        for(Literal b : state){
            contextString += b.toString() + " & ";
        }
        LogicalFormula context = ASSyntax.parseFormula(contextString.substring(0,contextString.length()-3));

        PlanBodyImpl body = new PlanBodyImpl(PlanBody.BodyType.action, plan.getAction(0));
        body.add(new PlanBodyImpl(PlanBody.BodyType.achieve, Literal.parseLiteral("act")));

        Random r = new Random();
        String label = String.valueOf(r.nextDouble());
        jason.asSyntax.Plan p = new jason.asSyntax.Plan(new Pred(label),trigger,context,body);
        planLibrary.add(p);
        if(parentLabel != null){
            int size = planLibrary.get(parentLabel).getBody().getPlanSize();
            planLibrary.get(parentLabel).getBody().add(size-1, new PlanBodyImpl(PlanBody.BodyType.action, plan.getAction(0)));
        }
        if(plan.getIfStatements().size() == 1){
            return recursiveGenerate(plan.getIfStatements().get(0).getPlan(), plan.getIfStatements().get(0).getState(), planLibrary, parentLabel==null?new Pred(label):parentLabel);
        }
        for (Plan.IfStatement conditional : plan.getIfStatements()){
            planLibrary = recursiveGenerate(conditional.getPlan(), (Set<Literal>) conditional.getState(), planLibrary, null);
        }
        return planLibrary;
    }
}
