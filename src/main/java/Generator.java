import core.search.nondeterministic.Plan;
import core.search.nondeterministic.ResultsFunction;
import jason.RevisionFailedException;
import jason.asSemantics.Agent;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;
import jason.bb.BeliefBase;

import java.sql.SQLOutput;
import java.util.*;

public class Generator<T, A> {

    private Map<Set<T>, Integer> headerNumbers;
    private Map<Set<T>, Integer> callNumbers;

    private Set<T> initialState;
    private Plan<Set<T>, A> root;

    private String getHeader(Set<T> state, int num){
        String header = "+!plan" + num + ": ";
        String guards = "";
        for (T s : state) {
            guards += s.toString() + " & ";
        }
        header += guards.substring(0, guards.length() - 2) + " <-\n";

        return header;
    }

    public Generator(){
        this.headerNumbers = new HashMap<>();
        this.callNumbers = new HashMap<>();
    }

    public String generate(Plan<Set<T>, A> root, Set<T> initialState){
        root.setUpParentPlans();
        this.root = root;
        this.initialState = initialState;
        String script = "//Initial State\n";
        for (T s : initialState){
            script += s.toString() + "\n";
        }
        script += "!plan0.\n";
        return recursiveGenerate(root, initialState, 0, true, false, script);
    }

    private String recursiveGenerate(Plan<Set<T>, A> plan, Set<T> state, int num, boolean printHeader, boolean loop, String script){
        if(printHeader){
            script += getHeader(state, num);
            if(!loop)
                headerNumbers.put(state, num);
        }
        if(loop){
            Integer s = headerNumbers.get(state);
            if(s!=null){
                script += "    !plan" + s + ".\n\n";
                return script;
            }
        }
        for (int i=0; i < plan.size(); i++){
            if(plan.isActionStep(i)){
                script += "    ." + plan.getAction(i) + ";\n";
            }
        }

        if(plan.getIfStatements().size() == 1){
            return recursiveGenerate(plan.getIfStatements().get(0).getPlan(), plan.getIfStatements().get(0).getState(), num, false, false, script);
        }

        boolean firstFlag = true;
        num = callNumbers.size()+1;
        for (Plan.IfStatement conditional: plan.getIfStatements()) {
            if(firstFlag && !plan.isEmpty() && (!conditional.getPlan().isEmpty() || conditional.getPlan().isLoop())){
                firstFlag = false;
                if(callNumbers.containsKey(state)){
                    script += "    !plan" + callNumbers.get(state) + ".\n\n";
                    return script;
                } else {
                    callNumbers.put(state, num);
                    script += "    !plan" + num + ".\n\n";
                }
            }
            if(conditional.getPlan().isLoop()){
                Plan p;
                if(conditional.getState().equals(initialState)){
                    p = root;
                } else {
                    p = conditional.getPlan().getParentPlan((Set<T>) conditional.getState());

                }
                script = recursiveGenerate(p, (Set<T>) conditional.getState(), num, true, true, script);
            } else if(!conditional.getPlan().isEmpty()){
                script = recursiveGenerate(conditional.getPlan(), (Set<T>) conditional.getState(), num, true, false, script);
            }
        }
        return script;
    }
/*
    public void setup (Agent agent){
        PlanLibrary planLibrary = agent.getPL();
        List<jason.asSyntax.Plan> plans = new ArrayList<>();
        for(jason.asSyntax.Plan plan : planLibrary.getPlans()){
            if(plan.getAnnots().contains(Literal.parseLiteral("operator")))
                plans.add(plan);
        }
        plans.get(0).getContext()
                agent.getBB()
    }


    public ResultsFunction<Set<Literal>, Literal> getResult(List<jason.asSyntax.Plan> operators) {
        return (Set<Literal> state, Literal action) -> {
            List<Set<Literal>> results = new ArrayList<>();
            Agent a = new Agent();
            for(Literal lit : state) {
                try {
                    a.addBel(lit);
                } catch (RevisionFailedException e) {
                    e.printStackTrace();
                }
            }
            operators.stream()
                    .filter(op -> op.getTrigger().getAnnots().contains("operator") && op.isRelevant(new Trigger(Trigger.TEOperator.add, Trigger.TEType.achieve, action)) != null && op.getContext().logicalConsequence(a, null))
                    .forEach(op -> {
                        BeliefBase bb;
                        for (PlanBody body : (PlanBodyImpl) op.getBody()) {
                            if(body.getBodyTerm().getSrcInfo().toString().contains("∨")){
                                //List<LogicalFormula> tempResults = new ArrayList<>(results);
                                //look at implementation for the epistemic solvers that exist
                                for(String s : body.getBodyTerm().getSrcInfo().toString().split("∨")){
                                    List<Set<Literal>> tempResults = new ArrayList<>(results);
                                    for (Set<Literal> formula : tempResults) {
                                        for(String l : s.split("∧")){
                                            if(l.trim().contains("not "){
                                                formula.remove()
                                            }
                                        }
                                    }
                                }

                            }
                            if(body.getBodyType() == PlanBody.BodyType.addBel){

                            } else if (body.getBodyType() == PlanBody.BodyType.delBel){
                                state.
                            }
                        }


                    });
        };
    }

    public void generate2(Plan<Set<T>, A> plan, Set<T> init, boolean loop, Integer... num){
        if(init != null){
            plan.setUpParentPlans();
            System.out.println("//Initial State");
            for (T s : init){
                System.out.println(s.toString());
            }

            System.out.println("\n!plan0\n\n+!plan0 <-");
            planNumbers.put(init, 0);

        }
        int i = num.length > 0 ? num[0] : 1;
        if (plan == null) {
            return;
        }
        for (int j = 0; j < plan.size(); j++) {
            //Uses plan.getNextAction st the step is incremented properly.
            if (plan.isActionStep(j)) {
                System.out.println("    ." + plan.getAction(j) + ";");
            } else if (plan.getIfStatements().size() == 1){
                generate(plan.getIfStatements().get(0).getPlan(), null, false, i);
            }else {
                boolean first = true;

                for(Plan.IfStatement cond : plan.getIfStatements()){
                    //If Conditional has an empty and non-looping plan.
                    if(!cond.getPlan().isEmpty() || cond.getPlan().isLoop()) {
                        //Prints the plan call once if you know that the node is non-terminal
                        if(first){
                            System.out.printf("    !plan%d.\n\n", i);
                            first = false;
                        }
                        Object beliefObject = cond.getState();
                        Set<T> beliefs = (Set<T>) beliefObject;

                        if(!loop && !planNumbers.containsKey(beliefs)) {

                            System.out.printf("+!plan%d: ", i);
                            String guards = "";
                            for (T belief : beliefs) {
                                guards += belief.toString() + " & ";
                            }
                            System.out.println(guards.substring(0, guards.length() - 2) + " <-");
                            planNumbers.put(beliefs, i);



                            if (cond.getPlan().isLoop()){
                                Plan p = plan.getParentPlan((Set<T>) cond.getState());
                                p = p==null?plan:p;
                                generate(p, null, true, planNumbers.get(beliefs));
                            } else {
                                generate(cond.getPlan(), null, false, i+1);
                            }

                        }
                    }
                }
            }
        }
        if(init != null) {
            planNumbers.clear();
        }

    }*/
}


