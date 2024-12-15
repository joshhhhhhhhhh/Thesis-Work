package nd;

import core.search.nondeterministic.ResultsFunction;
import jason.JasonException;
import jason.RevisionFailedException;
import jason.asSemantics.Agent;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;
import jdk.jshell.EvalException;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Array;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class NonDeterministicValues {
    //List<Map<VarTerm, Term>> allPossibleVarCombinations;
    //Map<VarTerm, List<Term>> initialValues;

    //Shape: {cell: [c0, c1 ...], vacuum: [v0]}
    Map<Literal, List<Term>> objects;

    List<Literal> actions;

    Set<Term> goalState;
    List<Plan> operators;
    public Set<Literal> initialBeliefs;
    private static final String NO_CHANGE = "None";

    public NonDeterministicValues(List<Literal> beliefs, Set<Term> goalState, List<Plan> operators){
        this.initialBeliefs = new HashSet<Literal>();
        this.objects = new HashMap<>();
        Map<String, Literal> functors = new HashMap<>();
        for (Literal literal : beliefs) {
            if(literal.getFunctor().startsWith("object")) {
                if (functors.containsKey(literal.getTerm(0).toString())) {
                    List<Term> temp = objects.get(functors.get(literal.getTerm(0).toString()));
                    temp.add(literal.getTerm(1));
                    objects.put(functors.get(literal.getTerm(0).toString()), temp);
                } else {
                    functors.put(literal.getTerm(0).toString(), Literal.parseLiteral(literal.getTerm(0).toString()));
                    List<Term> temp = new ArrayList<Term>();
                    temp.add(literal.getTerm(1));
                    objects.put(functors.get(literal.getTerm(0).toString()), temp);
                }
            } else if (literal.getFunctor().startsWith("predicate")) {
                List<String> types = new ArrayList();
                for (int i=1; i<literal.getTerms().size(); i++){
                    types.add(literal.getTerm(i).toString());
                }
            }
        }
        for (Literal literal : beliefs) {
            if(literal.getFunctor().startsWith("object") || literal.getFunctor().startsWith("predicate"))
                continue;
            //if( (literal.getArity()!= 0) && (!literal.getFunctor().startsWith("des")) && literal.getAnnots().contains(Literal.parseLiteral("bel"))){
            //if( (literal.getArity()!= 0) && (!literal.getFunctor().startsWith("des")) && !Collections.disjoint(literal.getAnnots().stream().map(a -> ((Literal)a).getTerm(1)).toList(), objects.keySet())){
            //if(!literal.getAnnots("type").isEmpty()){
            //if(predicates.keySet().contains(literal.getFunctor())){
            //TODO Test that this works lmao
            if(!literal.hasTerm() || !objects.values().stream().filter(e -> e.contains(literal.getTerm(0))).toList().isEmpty()){
                literal.delSources();
                //if(literal.hasAnnot())
                //    literal.clearAnnots();
                this.initialBeliefs.add(literal);
            }
        }
        this.operators = operators;
        this.goalState = new HashSet<Term>(goalState);
        this.actions = setActions();
        //this.initialValues = ???
    }

    /*public NonDeterministicValues(Map<VarTerm, List<Term>> initialValues, Set<LogicalFormula> goalState, List<Literal> actions){
        //this.allPossibleVarCombinations = new ArrayList();
        this.initialValues = initialValues;
        this.actions = actions;
        //nestedLoop(0, List.copyOf(initialValues.keySet()), null);

        /*
        boolean flag = true;
        for(VarTerm variable : initialValues.keySet()){
            List<Term> beliefSet = initialValues.get(variable);

            //Initializing the first sets in the body
            if(flag){
                flag = false;
                for(Term bel : beliefSet){
                    HashMap h = new HashMap();
                    h.put(variable, bel);
                    this.allPossibleVarCombinations.add(h);
                }
                continue;
            }

            List temp = new ArrayList();
            this.allPossibleVarCombinations.stream().forEach(s -> temp.add(s.clone()));
            for(int j=0; j<beliefSet.size()-1; j++){
                this.allPossibleVarCombinations.addAll(temp);
            }
            for(int j=0; j<this.allPossibleVarCombinations.size(); j++){
                this.allPossibleVarCombinations.get(j).put(variable, beliefSet.get((j*beliefSet.size())/this.allPossibleVarCombinations.size()));
            }
        }
        this.goalState = new HashSet<>(goalState);
        //System.out.println(this.allPossibleVarCombinations);
        //System.out.println(this.allPossibleVarCombinations.size());
    }*/
    public List<Literal> getActions(Object state) {
        return this.actions;
    }

    public List<Literal> setActions() {
        Set<Literal> ret = new HashSet<>();


        for(Plan op : operators){
            List<Term> tempAnnots = op.getLabel().getAnnots().getAsList().stream().filter(t->t.toString().contains("type(")).toList();
            //List<VarTerm> tempVariables = op.getTrigger().getLiteral().getTerms().stream().map(t -> (VarTerm)t).toList();

            List<Term> types = new ArrayList<>();
            List<VarTerm> vars = new ArrayList<>();
            //System.out.println("VARS: " + tempVariables + " TYPES: " + op.getLabel().getTerms());
            for(Term var : tempAnnots){
                Literal lit = (Literal)var;
                if(var.toString().equals(lit.getTerm(0).toString())){
                    if(!lit.getTerm(2).toString().equals("temp")){
                        types.add(lit.getTerm(1));
                        vars.add((VarTerm) lit.getTerm(0));
                    }
                }


                /*if(!tempTypes.get(i).toString().contains("temp")){
                    types.add(tempTypes.get(i));
                    vars.add(tempVariables.get(i));
                }*/
                    //types.add(Literal.parseLiteral(t.toString().replace("temp", "")));
            }
            List<Map<VarTerm, Term>> allPossibleVarCombinations = new ArrayList<>();
            nestedLoop(0, types, vars, null, allPossibleVarCombinations);

            Literal lit = Literal.parseLiteral(op.getTrigger().getLiteral().getFunctor());
            for(VarTerm var : vars){
                lit.addTerm(var);
            }

            for(Map<VarTerm, Term> mapping : allPossibleVarCombinations){
                if(mapping == null){
                    ret.add(lit);
                } else {
                    Unifier u = new Unifier();
                    for(VarTerm key : mapping.keySet()){
                        u.bind(key, mapping.get(key));
                    }
                    ret.add((Literal) lit.capply(u));
                }
            }
        }
        return ret.stream().toList();
    }


    private void nestedLoop(int numVars, List<Term> types, List<VarTerm> variables, Map<VarTerm, Term> vals, List<Map<VarTerm, Term>> allPossibleVarCombinations){
        if(numVars < types.size()){
            for(Term value : objects.get(types.get(numVars))){
                Map<VarTerm, Term> newMap= new HashMap<>();
                if(vals != null)
                    newMap.putAll(vals);
                newMap.put(variables.get(numVars), value);
                nestedLoop(numVars+1, types, variables, newMap, allPossibleVarCombinations);
            }
        } else {
            allPossibleVarCombinations.add(vals);
        }
    }

    private boolean EvaluateExpression(Term unifiedTerm, Set<Literal> state) throws JasonException {
        if(unifiedTerm instanceof LogExpr){
            if(((LogExpr) unifiedTerm).getOp().equals(LogExpr.LogicalOp.not))
                return !EvaluateExpression(((LogExpr) unifiedTerm).getLHS(), state);
            else if(((LogExpr) unifiedTerm).getOp().equals(LogExpr.LogicalOp.and))
                return EvaluateExpression(((LogExpr) unifiedTerm).getLHS(), state) && EvaluateExpression(((LogExpr) unifiedTerm).getRHS(), state);
        } else if(unifiedTerm instanceof RelExpr){
            return Boolean.TRUE.equals(evaluateRelativeExpression((RelExpr) unifiedTerm));
        } else if(unifiedTerm instanceof Literal){
            return state.contains(unifiedTerm);
        }
        throw new JasonException("Evaluation failed for " + unifiedTerm + "of type: " + unifiedTerm.getClass().getTypeName());
    }

    private Boolean evaluateRelativeExpression(RelExpr expr){
        switch(expr.getOp()){
            case none:
                break;
            case gt:
                if(expr.getLHS().compareTo(expr.getRHS()) > 0) return true;
                else return false;
            case gte:
                if(expr.getLHS().compareTo(expr.getRHS()) >= 0) return true;
                else return false;
            case lt:
                if(expr.getLHS().compareTo(expr.getRHS()) < 0) return true;
                else return false;
            case lte:
                if(expr.getLHS().compareTo(expr.getRHS()) <= 0) return true;
                else return false;
            case eq:
                if(expr.getLHS().equals(expr.getRHS())) return true;
                else return false;
            case dif:
                if(!expr.getLHS().equals(expr.getRHS())) return true;
                else return false;
        }
        return null;
    }

    private void applyExprToState(Term unifiedTerm, Set<Literal> state, boolean add) throws JasonException{
         if(unifiedTerm instanceof LogExpr){
             if(((LogExpr) unifiedTerm).getOp().equals(LogExpr.LogicalOp.not))
                 applyExprToState(((LogExpr) unifiedTerm).getLHS(), state, !add);
             else if(((LogExpr) unifiedTerm).getOp().equals(LogExpr.LogicalOp.and)){
                 applyExprToState(((LogExpr) unifiedTerm).getLHS(), state, add);
                 applyExprToState(((LogExpr) unifiedTerm).getRHS(), state, add);
             } else
                 throw new JasonException("Application failed for " + unifiedTerm + "of type: " + unifiedTerm.getClass().getTypeName());

         } else if(unifiedTerm instanceof Literal){
             if(((Literal)unifiedTerm).getFunctor().equals(NO_CHANGE))
                 return;
             else if(add){
                state.add((Literal)unifiedTerm);
             } else {
                state.remove((Literal)unifiedTerm);
             }
        } else
             throw new JasonException("Application failed for " + unifiedTerm + "of type: " + unifiedTerm.getClass().getTypeName());
    }

    public boolean testGoalFunction(Object state){
        Set<Literal> currState = (Set<Literal>) state;
        boolean flag = true;
        for (Term goal : this.goalState){
            if(goal instanceof LogExpr){
                if(((LogExpr) goal).getOp().equals(LogExpr.LogicalOp.not)){
                    if(((LogExpr) goal).getLHS() instanceof Literal){
                        if(currState.contains(goal)){
                            flag = false;
                        }
                    } else {
                        flag = false;
                    }
                } else {
                    flag = false;
                }
            } else if(goal instanceof Literal){
                if(!currState.contains(goal)){
                    flag = false;
                }
            } else {
                flag = false;
            }
        }
        return flag;
    }


    public ResultsFunction<Set<Literal>, Literal> results() {
        return (Set<Literal> state, Literal action) -> {

            List<Plan> viableOperators = new ArrayList<>();
            /*List<Set<Literal>> validStates = new ArrayList<>();
            for(HashMap<VarTerm, Term> potentialState : allPossibleVarCombinations){
                if(potentialState.containsAll(state))
                    validStates.add(potentialState);
            }*/
            this.operators.stream().filter(op -> op.getTrigger().getLiteral().getFunctor().equals(action.getFunctor()))
                    .forEach(viableOperators::add);
            for(Plan op : viableOperators){
                List<Term> tempAnnots = op.getLabel().getAnnots().getAsList().stream().filter(t->t.toString().contains("type(")).toList();
                //List<VarTerm> tempVariables = op.getTrigger().getLiteral().getTerms().stream().map(t -> (VarTerm)t).toList();

                List<Term> types = new ArrayList<>();
                List<VarTerm> mandatoryVars = new ArrayList<>();
                List<VarTerm> notMandatoryVars = new ArrayList<>();

                for(Term var : tempAnnots) {
                    Literal lit = (Literal) var;
                    types.add(lit.getTerm(1));
                    if (!lit.getTerm(2).toString().equals("temp")) {
                        mandatoryVars.add((VarTerm) lit.getTerm(0));
                    } else {
                        notMandatoryVars.add((VarTerm) lit.getTerm(0));
                    }
                }
                System.out.println("TYPES: " + types);
                System.out.println("Mandatory TYpes: " + mandatoryVars);
                System.out.println("NonMandatoryTypes: " + notMandatoryVars);

                /*for(int i=0; i<tempVariables.size();i++){
                    if(!tempTypes.get(i).toString().contains("temp")){
                        mandatoryVars.add(tempVariables.get(i));
                    } else {
                        types.add(Literal.parseLiteral(tempTypes.get(0).toString().replace("temp", "")));
                        notMandatoryVars.add(tempVariables.get(i));
                    }
                    //types.add(Literal.parseLiteral(t.toString().replace("temp", "")));
                }*/

                List<Term> values = action.getTerms();
                Unifier unifier = new Unifier();
                if(values != null){
                    for(int i=0; i<values.size(); i++){
                        unifier.bind(mandatoryVars.get(i), values.get(i));
                    }
                }
                LogicalFormula context = op.getContext();
                Term semiUnifiedContext = context.capply(unifier);

                List<Map<VarTerm, Term>> allPossibleVarCombinations = new ArrayList<>();
                nestedLoop(0, types, notMandatoryVars, null, allPossibleVarCombinations);

                Unifier finalUnifier = null;
                for(Map<VarTerm, Term> combination : allPossibleVarCombinations){
                    Unifier tempUnifier = new Unifier();
                    for(VarTerm key : combination.keySet()){
                        tempUnifier.bind(key, combination.get(key));
                    }

                    Term unifiedContext = semiUnifiedContext.capply(tempUnifier);
                    try{
                        if(EvaluateExpression(unifiedContext, state)) {
                            //System.out.println(state + " | " + unifiedContext);
                            finalUnifier = tempUnifier;
                            break;
                        }
                    } catch (JasonException e){
                        e.printStackTrace();
                    }
                }

                if(finalUnifier == null)
                    continue;
                /*
                List<Term> types = op.getLabel().getAnnots().getAsList().stream().filter(t->!t.toString().contains("source(") && !t.toString().contains("url(")).toList();

                List<VarTerm> variables = op.getTrigger().getLiteral().getTerms().stream().map(t -> (VarTerm)t).toList();
                List<Map<VarTerm, Term>> allPossibleVarCombinations = new ArrayList<>();
                nestedLoop(0, types, variables, null, allPossibleVarCombinations);

                LogicalFormula context = op.getContext();
                boolean passedContext = false;
                Unifier unifier = new Unifier();

                for(Map<VarTerm, Term> combination : allPossibleVarCombinations){
                    Unifier tempUnifier = new Unifier();
                    for(VarTerm key : combination.keySet()){
                        tempUnifier.bind(key, combination.get(key));
                    }

                    Term unifiedContext = context.capply(tempUnifier);
                    try{
                        if(EvaluateExpression(unifiedContext, state)) {
                            //System.out.println(state + " | " + unifiedContext);
                            passedContext = true;
                            unifier = tempUnifier;
                            break;
                        }
                    } catch (JasonException e){
                        e.printStackTrace();
                    }
                }*/

                /*
                for(Literal a : actions){
                    if(a.getFunctor().equals(action.getFunctor())){
                        terms = a.getTerms();
                    }
                }

                Unifier unifier = new Unifier();
                if(terms != null && terms.size() > 0){
                    for(int i=0; i<terms.size();i++){
                        unifier.bind((VarTerm)terms.get(i), action.getTerm(i));
                    }
                }
                Unifier opOnlyUnifier = new Unifier();
                Term semiUnifiedContext = op.getContext().capply(unifier);


                //This is for the vars which are part of the operator but not the action
                List<Term> opTerms = op.getTrigger().getLiteral().getTerms();
                List<VarTerm> varTerms = new ArrayList<>();
                List<Map<VarTerm, Term>> allPossibleVarCombinations = new ArrayList<>();
                if(opTerms != null){
                    for(Term ct : opTerms){
                        if(!action.hasVar((VarTerm)ct, new Unifier())){
                            varTerms.add((VarTerm)ct);
                        }
                    }
                }


                boolean passedContext = false;

                if(varTerms.isEmpty()){
                    try{
                        if(!EvaluateExpression(semiUnifiedContext, state)) {
                            continue;
                        }
                        passedContext = true;
                    } catch (JasonException e){
                        e.printStackTrace();
                    }
                } else {
                    nestedLoop(0,varTerms,null,allPossibleVarCombinations);
                    for(Map<VarTerm, Term> combination : allPossibleVarCombinations){
                        Unifier tempUnifier = new Unifier();
                        for(VarTerm key : combination.keySet()){
                            tempUnifier.bind(key, combination.get(key));
                        }

                        Term unifiedContext = semiUnifiedContext.capply(tempUnifier);
                        try{
                            if(EvaluateExpression(unifiedContext, state)) {
                                passedContext = true;
                                opOnlyUnifier = tempUnifier;
                                break;
                            }
                        } catch (JasonException e){
                            e.printStackTrace();
                        }
                    }
                }
                if(!passedContext)
                    continue;*/
                //System.out.println("Valid Values | " + validValues);
                //System.out.println("Current State | " + state);
                //if(validValues.size() != 1 && (validValues.size() == 0 || validValues.size() != allPossibleVarCombinations.size()))
                //    continue;
                //If all of the ranges are valid, the context is purely literals, which means the body must also be purely literals.
                // This also means that they all passed, so it is valid and can move on to the next step

                PlanBody curr = op.getBody();

                /*Unifier unifier = new Unifier();
                for(VarTerm key : validValues.stream().toList().get(0).keySet()){
                    unifier.bind(key, validValues.stream().toList().get(0).get(key));
                }*/

                Set<Set<Literal>> results = new HashSet<>();
                while(true){
                    if(curr == null)
                        break;

                    Set<Literal> s = new HashSet<>();
                    for (Literal b : state){
                        s.add((Literal) b.clone());
                    }
                    Term unifiedWorld = curr.getBodyTerm().capply(finalUnifier);
                    try {
                        applyExprToState(unifiedWorld, s, true);
                    } catch (JasonException e) {
                        e.printStackTrace();
                    }
                    results.add(s);

                    curr = curr.getBodyNext();
                }
                return new ArrayList<>(results);
            }
            return new ArrayList<>();
        };
    }
}
