import core.search.nondeterministic.ResultsFunction;
import jason.JasonException;
import jason.RevisionFailedException;
import jason.asSemantics.Agent;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;
import jdk.jshell.EvalException;
import peleus.PlanContextExtractor;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Array;
import java.util.*;
import java.util.logging.Logger;

public class NonDeterministicValues {
    //List<Map<VarTerm, Term>> allPossibleVarCombinations;
    Map<VarTerm, List<Term>> initialValues;
    Set<LogicalFormula> goalState;
    List<Literal> actions;
    private static final String NO_CHANGE = "None";
    public NonDeterministicValues(Map<VarTerm, List<Term>> initialValues, Set<LogicalFormula> goalState, List<Literal> actions){
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
        }*/
        this.goalState = new HashSet<>(goalState);
        //System.out.println(this.allPossibleVarCombinations);
        //System.out.println(this.allPossibleVarCombinations.size());
    }

    public List<Literal> getActions(Object state) {
        List<Literal> ret = new ArrayList<>();
        for(LogicalFormula action : actions){
            List<VarTerm> vars = new ArrayList<>();
            for(VarTerm var : initialValues.keySet()){
                if(action.hasVar(var, new Unifier())){
                    vars.add(var);
                }
            }
            List<Map<VarTerm, Term>> allPossibleVarCombinations = new ArrayList<>();
            nestedLoop(0, vars, null, allPossibleVarCombinations);

            for(Map<VarTerm, Term> mapping : allPossibleVarCombinations){
                Unifier u = new Unifier();
                for(VarTerm key : mapping.keySet()){
                    u.bind(key, mapping.get(key));
                }
                ret.add((Literal) action.capply(u));
            }
        }
        return ret;
    }

    private void nestedLoop(int numVars, List<VarTerm> keys, Map<VarTerm, Term> vals, List<Map<VarTerm, Term>> allPossibleVarCombinations){
        if(numVars < keys.size()){
            for(Term value : initialValues.get(keys.get(numVars))){
                Map<VarTerm, Term> newMap= new HashMap<>();
                if(vals != null)
                    newMap.putAll(vals);
                newMap.put(keys.get(numVars), value);
                nestedLoop(numVars+1, keys, newMap, allPossibleVarCombinations);
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
        for (LogicalFormula goal : this.goalState){
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


    public ResultsFunction<Set<Literal>, Literal> results(List<Plan> operators) {
        return (Set<Literal> state, Literal action) -> {
            List<Plan> viableOperators = new ArrayList<>();
            /*List<Set<Literal>> validStates = new ArrayList<>();
            for(HashMap<VarTerm, Term> potentialState : allPossibleVarCombinations){
                if(potentialState.containsAll(state))
                    validStates.add(potentialState);
            }*/
            operators.stream().filter(op -> op.getTrigger().getLiteral().getFunctor().equals(action.getFunctor()))
                    .forEach(viableOperators::add);
            for(Plan op : viableOperators){
                /*
                List<VarTerm> terms = new ArrayList<>();
                List<Map<VarTerm, Term>> allPossibleVarCombinations = new ArrayList<>();
                for(VarTerm key : initialValues.keySet()){
                    if(op.getContext().hasVar(key, new Unifier())){
                        terms.add(key);
                    }
                }
                nestedLoop(0, terms, null, allPossibleVarCombinations);*/
                List<Term> terms = new ArrayList<>();
                for(Literal a : actions){
                    if(a.getFunctor().equals(action.getFunctor())){
                        terms = a.getTerms();
                    }
                }
                Unifier unifier = new Unifier();
                for(int i=0; i<terms.size();i++){
                    unifier.bind((VarTerm)terms.get(i), action.getTerm(i));
                }
                /*
                //for(Map<VarTerm, Term> possibility : allPossibleVarCombinations){
                    Unifier unifier = new Unifier();
                    for(VarTerm key : possibility.keySet()){
                        unifier.bind(key, possibility.get(key));
                    }*/
                Term unifiedContext = op.getContext().capply(unifier);
                try{
                    if(!EvaluateExpression(unifiedContext, state)) {
                        continue;
                    }
                } catch (JasonException e){
                    e.printStackTrace();
                }
                //}
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

                ArrayList<Set<Literal>> results = new ArrayList<>();
                while(true){
                    if(curr == null)
                        break;

                    Set<Literal> s = new HashSet<>();
                    for (Literal b : state){
                        s.add((Literal) b.clone());
                    }
                    Term unifiedWorld = curr.getBodyTerm().capply(unifier);
                    try {
                        applyExprToState(unifiedWorld, s, true);
                    } catch (JasonException e) {
                        e.printStackTrace();
                    }
                    results.add(s);

                    curr = curr.getBodyNext();
                }
                return results;
            }
            return new ArrayList<>();
        };
    }
}
