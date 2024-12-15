package nd;

import core.search.nondeterministic.AndOrSearch;
import core.search.nondeterministic.NondeterministicProblem;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.InternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;
import jason.bb.BeliefBase;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.logging.Logger;


public class non_deterministic_planning_action extends DefaultInternalAction {


    private static final Logger logger = Logger.getLogger(InternalAction.class.getName());

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {

        //First check that the action was properly invoked with an AgentSpeak
        //list as its parameter.
        System.out.println("START");
        logger.info("START-logger");
        if(args.length < 1) {
            logger.info("plan action must have at least one parameter");
            return false;
        }
        if(!(args[0] instanceof ListTerm listTerm)){
            logger.info("plan action requires a list of literals as its parameter");
            return false;
        }
        String planner;
        if(args.length >= 2){
            planner = args[1].toString().toLowerCase().trim();
        } else {
            planner = "default";
        }

        Set<Term> goals = new HashSet<Term>(listTerm.getAsList());

        //Extract the literals in the belief base to be used
        //as the initial state for the planning problem
        BeliefBase beliefBase = ts.getAg().getBB();
        Iterator<Literal> beliefsIterator = beliefBase.iterator();
        List<Literal> beliefs = new ArrayList<Literal>();
        while(beliefsIterator.hasNext()) {
            Literal belief = beliefsIterator.next();
            beliefs.add(belief);
        }
        logger.info("beliefBase: "+beliefBase);

        //Extract the plans from the plan library to generate
        //STRIPS operators in the conversion process
        PlanLibrary planLibrary = ts.getAg().getPL();
        List<Plan> plans = planLibrary.clone().getPlans();
        plans.removeIf(plan -> !plan.getLabel().getFunctor().contains("action"));

        logger.info("planLibrary: "+planLibrary);

        NonDeterministicValues nd = new NonDeterministicValues(beliefs, goals, plans);

        System.out.println("BELIEFS: " + nd.initialBeliefs);
        System.out.println("GOALS: " + nd.goalState);
        System.out.println("OPERATORS(" + nd.operators.size() + "): " + nd.operators);
        System.out.println("OBJECTS: " + nd.objects);

        Map<String, List<Term>> terms = new HashMap<>();
        Optional plan;
        if(planner.equals("default")){
            NondeterministicProblem problem = new NondeterministicProblem(
                    nd.initialBeliefs,
                    nd::getActions,
                    nd.results(),
                    nd::testGoalFunction);
            AndOrSearch<List<Literal>, Literal> search = new AndOrSearch<>();
            System.out.println("Setup Done");
            plan = search.search(problem);
            System.out.println("Search Done");
            for(Plan op : nd.operators){
                List<Term> types = op.getLabel().getAnnots().getAsList().stream().filter(t->!t.toString().contains("source(") && !t.toString().contains("url(")).toList();
                terms.put(op.getTrigger().getLiteral().getFunctor(), types);
            }
        } else {
            AgentSpeakToPDDL agentSpeakToPDDL = new AgentSpeakToPDDL();
            agentSpeakToPDDL.generatePDDL(nd);
            String[] command;
            if(planner.equals("mynd")){
                command = new String[]{
                     "python", "../MyNDPlanner/translator-fond/translate.py", "domain.pddl", "task.pddl",
                        "&&", "java", "../MyNDPlanner/src/mynd.MyNDPlanner", "-dumpPlan",
                        "../MyNDPlanner/output.sas"
                };

                Process proc = new ProcessBuilder(command).start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));

                String line = "";
                while((line = reader.readLine()) != null){
                    System.out.println(line+"\n");
                }

                proc.waitFor();
                plan = null;

                //"python ../MyNDPlanner/translator-fond/translate.py domain.pddl task.pddl  && java ../MyNDPlanner/mynd.MyNDPlanner -laostar -ff -dumpPlan output.sas > plan.txt";
            } else {
                logger.info("Planner not Recognized: " + planner);
                return false;
            }

        }


        Generator_V2 g = new Generator_V2(terms);
        System.out.println("Generation Done");
        g.generate((core.search.nondeterministic.Plan<Set<Literal>, Literal>) plan.get(), nd.initialBeliefs, planLibrary);
        //Reducer.reduce(planLibrary);
        logger.info("New planLibrary: "+planLibrary);
        for(Plan p : planLibrary.getPlans().stream().filter(e -> e.getLabel().toString().contains("Generated")).toList()){
            logger.info(p.toASString());
        }
        ts.getC().addAchvGoal(Literal.parseLiteral("act"), null);
        nd.results().results(nd.initialBeliefs, Literal.parseLiteral("kjaoi"));
        return true;
    }
}
