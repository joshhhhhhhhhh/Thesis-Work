package nd;

import jason.JasonException;
import jason.asSyntax.*;

import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class OutputParser {
    public OutputParser() {
    }

    public static LinkedHashMap<List<Literal>, Literal> parsePRP(String out) throws ParseException {
        String[] policy = out.split("Policy:")[1].replaceAll("strong_negate_", "~").split("\n");
        LinkedHashMap<List<Literal>, Literal> ret = new LinkedHashMap<>();
        for(int i=1; i<policy.length-1; i++){
            if (policy[i].startsWith("If holds: ") && policy[i+1].startsWith("Execute")){
                List<Literal> preds = new ArrayList<>();
                String[] predStrings = policy[i].split(": ")[1].split("/");
                for(String str : predStrings){
                    preds.add(Literal.parseLiteral(str.replace("\n", "").replace("()", "").trim()));
                }
                String[] actionStrings = policy[i+1].split(": ")[1].split(" /")[0].split(" ");
                String action = actionStrings[0];
                if(actionStrings.length > 1){
                    action = actionStrings[0] + "(";

                    for(int j=1; j<actionStrings.length; j++){
                        action += actionStrings[j] + ",";
                    }
                    action = action.substring(0, action.length()-1);
                    action += ")";

                }
                ret.put(preds, Literal.parseLiteral(action));
            }
        }
        return ret;
    }


    public static LinkedHashMap<List<Literal>, Literal> parseMyND(String out) throws ParseException {
        String[] policy = out.split("Number of sensing applications in policy")[1].replaceAll("strong_negate_", "~").split("\n");

        List<Literal> preds = new ArrayList<>();
        List<Literal> actions = new ArrayList<>();

        //Start at 1 bc the first line is useless.
        for (int i = 1; i < policy.length; i++) {
            if(policy[i].length() == 0){
                continue;
            }
            if (Character.isDigit(policy[i].charAt(0))) {
                if (preds.isEmpty()) {
                    String[] stringPreds = policy[i].replaceAll("\\)", "").replaceAll("\n", "").replaceAll("\\(\\)", "").split("\\(");
                    //individual stringPreds is of form "clean c0"
                    for (int j = 1; j < stringPreds.length; j++) {
                        String[] stringLit = stringPreds[j].split(" ");
                        String str = stringLit[0];
                        if (stringLit.length != 1) {
                            str += "(";
                            for (int k = 1; k < stringLit.length; k++) {
                                str += stringLit[k] + ",";
                            }
                            str = str.substring(0, str.length()-1);
                            str += ")";
                        }
                        preds.add(Literal.parseLiteral(str));
                    }
                } else if (actions.isEmpty()) {
                    String[] stringPreds = policy[i].replaceAll("\\)", "").replaceAll("\n", "").replaceAll("\\(\\)", "").split("\\(");
                    //individual stringPreds is of form "clean c0"
                    for (int j = 1; j < stringPreds.length; j++) {
                        String[] stringLit = stringPreds[j].split(" ");
                        String str = stringLit[0];
                        if (stringLit.length != 1) {
                            str += "(";
                            for (int k = 1; k < stringLit.length; k++) {
                                str += stringLit[k] + ",";
                            }
                            str = str.substring(0, str.length()-1);
                            str += ")";
                        }
                        actions.add(Literal.parseLiteral(str));
                    }
                }
            } else if (policy[i].startsWith("policy")) {
                if (preds.isEmpty() || actions.isEmpty()) {
                    throw new ParseException("Unable to parse MyND policy:" + policy, 1);
                }
                String[] str = policy[i].split(" ");
                int numberOfPreds = 0;
                int count = 0;
                List<Literal> tempPreds = new ArrayList<>();
                LinkedHashMap<List<Literal>, Literal> ret = new LinkedHashMap<>();
                for (int j = 2; j < str.length; j++) {
                    if(count == 0){
                        numberOfPreds = Integer.parseInt(str[j]);
                        count++;
                    } else if (count == numberOfPreds+1) {
                        ret.put(new ArrayList<>(tempPreds), actions.get(Integer.parseInt(str[j])));
                        tempPreds.clear();
                        count = 0;
                    } else {
                        tempPreds.add(preds.get(Integer.parseInt(str[j])));
                        count++;
                    }
                }
                return ret;
            }
        }
        throw new ParseException("Unable to parse MyND policy:" + policy, 1);
    }

    public static LinkedHashMap<List<Literal>, Literal> parsePaladinus(String out) throws ParseException {
        String[] policy = out.split("# Policy:")[1].replaceAll("\\(\\)", "").replaceAll("strong_negate_", "~").split("\n");
        LinkedHashMap<List<Literal>, Literal> ret = new LinkedHashMap<>();
        for (int i = 0; i < policy.length; i++) {
            if (policy[i].startsWith("If holds:")) {
                //pred is of form (not (clean c1 c2))
                //             or (clean c1 c2)
                //New context will hold without NOT preds
                List<Literal> tempPreds = new ArrayList<>();
                for (String pred : policy[i].split(": ")[1].replaceAll("\n", "").split(", ")) {
                    if (!pred.contains("(not (")) {
                        String[] pieces = pred.replaceAll("\\(", "").replaceAll("\\)", "").split(" ");
                        String str = pieces[0];
                        if (pieces.length > 1) {
                            str += "(";
                            for (int j = 1; j < pieces.length; j++) {
                                str += pieces[j] + ",";
                            }
                            str = str.substring(0, str.length()-1);
                            str += ")";

                        }
                        tempPreds.add(Literal.parseLiteral(str));
                    }
                }
                String[] action = policy[i + 1].split(": ")[1].split(" ");
                String str = action[0];
                if (action.length > 1) {
                    str += "(";

                    for (int j = 1; j < action.length; j++) {
                        str += action[j] + ",";
                    }
                    str = str.substring(0, str.length()-1);
                    str += ")";
                }
                ret.put(tempPreds, Literal.parseLiteral(str));
            }
        }
        return ret;
    }

    public static LinkedHashMap<List<Literal>, Literal> parseFONDSAT(String out) throws ParseException {
        String[] policy = out.replaceAll("\\(\\)", "").replaceAll("strong_negate_", "~").split("\n");
        Map<String, List<Literal>> preds = new HashMap<>();

        //{n0 : right(c0)}
        LinkedHashMap<String, String> actions = new LinkedHashMap<>();
        LinkedHashMap<List<Literal>, Literal> ret = new LinkedHashMap<>();
        //Modes: preds, actions
        String mode = "";
        for (String str : policy) {


            if (mode.equals("preds") && str.startsWith("Atom")) {
                String[] predWithState = str.split(" ");
                String state = predWithState[2].replaceAll("\\(", "").replaceAll("\\)", "");
                if (!preds.containsKey(state)) {
                    preds.put(state, new ArrayList<>());
                }
                preds.get(state).add(Literal.parseLiteral(predWithState[1]));
            }

            if (mode.equals("actions")) {
                if (!str.startsWith("_") && !str.startsWith("(")) {
                    break;
                } else if (str.startsWith("(n")) {
                    String[] action = str.replaceFirst("\\(", "").split(",", 2);
                    if (actions.containsKey(action[0])) {
                        String currentAction = action[1].substring(0, action[1].length() - 1).replaceFirst("_DETDUP_\\d+", "");
                        if (currentAction.length() > actions.get(action[0]).length()) {
                            actions.put(action[0], currentAction);
                        }
                    } else {
                        actions.put(action[0], action[1].substring(0, action[1].length() - 1).replaceFirst("_DETDUP_\\d+", ""));
                    }
                }
            }

            if (str.equals("Atom (CS)")) {
                mode = "preds";
            } else if (str.equals("(CS, Action with arguments)")) {
                mode = "actions";
            }
        }
        System.out.println("ACTIONS: " + actions);
        for(String state : actions.keySet()){
            ret.put(preds.get(state), Literal.parseLiteral(actions.get(state)));
        }

        System.out.println("PREDS: " + preds);
        System.out.println("HASHMAP: " + ret);

        return ret;

    }

    public static void addPlansFromPlannerToLibrary(LinkedHashMap<List<Literal>, Literal> plans, PlanLibrary lib, NonDeterministicValues nd) throws jason.asSyntax.parser.ParseException, JasonException {
        for(List<Literal> contextList : plans.keySet()){



            Trigger trigger = new Trigger(Trigger.TEOperator.add, Trigger.TEType.achieve, Literal.parseLiteral("act"));

            String contextString = "";
            for(Literal b : contextList){
                contextString += b.toString() + " & ";
            }
            LogicalFormula context = ASSyntax.parseFormula(contextString.substring(0,contextString.length()-3));


            if(plans.get(contextList).toString().equals("goal")){
                PlanBodyImpl body = new PlanBodyImpl();
                Random r = new Random();

                String label = "Generated" + String.valueOf(r.nextDouble());

                jason.asSyntax.Plan p = new jason.asSyntax.Plan(new Pred(label),trigger,context,body);
                lib.add(p);
                continue;
            }

            Literal action = plans.get(contextList);
            Literal newLiteral = null;
            for(Plan op : nd.operators){
                //System.out.println("ACTION LIT: "+ action + " OP LIT: " + op.getTrigger().getLiteral().getFunctor());
                if(op.getTrigger().getLiteral().getFunctor().equalsIgnoreCase(action.getFunctor())){
                    newLiteral = Literal.parseLiteral(action.getFunctor().toLowerCase().replace(op.getLabel().getFunctor().toLowerCase(), ""));
                    //System.out.println("ADDING PLAN LIT: "+ newLiteral.toString());
                    if(!op.getTrigger().getLiteral().hasTerm()){
                       continue;
                    }
                    for(int i=0; i<op.getTrigger().getLiteral().getTerms().size(); i++){
                        for(Term type: op.getLabel().getAnnots("type").getAsList()){
                            if(type instanceof Literal){
                                Literal lit = (Literal)type;
                                if(op.getTrigger().getLiteral().getTerms().get(i).toString().equals(lit.getTerm(0).toString()) && !lit.getTerm(2).toString().equals("temp")){
                                    newLiteral.addTerm(action.getTerm(i));
                                }
                            }
                        }
                    }
                    break;
                }
            }
            PlanBodyImpl body = new PlanBodyImpl(PlanBody.BodyType.action, newLiteral);
            body.add(new PlanBodyImpl(PlanBody.BodyType.achieve, Literal.parseLiteral("act")));

            Random r = new Random();
            String label = "Generated" + String.valueOf(r.nextDouble());

            jason.asSyntax.Plan p = new jason.asSyntax.Plan(new Pred(label),trigger,context,body);
            lib.add(p);
        }
    }

}