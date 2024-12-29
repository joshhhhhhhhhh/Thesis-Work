package nd;
import fr.uga.pddl4j.parser.*;
import fr.uga.pddl4j.problem.DefaultProblem;
import fr.uga.pddl4j.problem.Problem;
import jason.JasonException;
import jason.asSyntax.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class AgentSpeakToPDDL {

    Map<String, List<String>> predicates;
    ParsedDomain domain;
    ParsedProblem problem;

    public AgentSpeakToPDDL(){
        this.predicates = new HashMap<>();
        this.domain = new DefaultParsedProblem(new Symbol<>(SymbolType.DOMAIN, "d1"));
        this.problem = new DefaultParsedProblem(new Symbol<String>(SymbolType.DOMAIN, "domain"));
        problem.setDomainName(new Symbol<>(SymbolType.DOMAIN, "d1"));
    }
    public void generatePDDL(NonDeterministicValues nd) {
        try {
            generateDomainAndProblem(nd);
        } catch (JasonException e) {
            System.out.println("Could Not Creating PDDL Files: " + e.getMessage());
        }
    }

    private void generateDomainAndProblem(NonDeterministicValues nd) throws JasonException {


        domain.addRequirement(RequireKey.EQUALITY);
        domain.addRequirement(RequireKey.STRIPS);
        domain.addRequirement(RequireKey.TYPING);

        //Adds the types
        for(Literal type : nd.objects.keySet()){
            domain.addType(new TypedSymbol<>(new Symbol<>(SymbolType.TYPE, type.toString())));
        }

        //objects
        for(Map.Entry<Literal, List<Term>> entry : nd.objects.entrySet()){
            Literal type = entry.getKey();
            List<Term> objects = entry.getValue();
            for(Term object : objects) {
                TypedSymbol t = new TypedSymbol<>(SymbolType.CONSTANT, object.toString());
                t.addType(new Symbol(SymbolType.TYPE, type.toString()));
                problem.addObject(t);
            }
        }

        //Adds Actions :)
        for(Plan op : nd.operators){
            System.out.println("LITERAL: " + op.getTrigger().getLiteral());
            List<String> actionVariables;
            if(op.getTrigger().getLiteral().hasTerm()){
                actionVariables = op.getTrigger().getLiteral().getTerms().stream().map(Object::toString).toList();
            } else {
                actionVariables = Collections.emptyList();
            }
            List<Term> types = op.getLabel().getAnnots().getAsList().stream().filter(t->t.toString().contains("type(")).toList();

            Map<String, String> paramsWithTypes = new HashMap<>();
            List<TypedSymbol<String>> params = new ArrayList<>();
            for(String var : actionVariables){
                TypedSymbol param = new TypedSymbol(SymbolType.VARIABLE, "?"+var);
                for(Term t : types){
                    Literal lit = (Literal)t;
                    if(lit.getTerm(0).toString().equals(var)){
                        param.addType(new Symbol(SymbolType.TYPE, lit.getTerm(1).toString()));
                        paramsWithTypes.put(var, lit.getTerm(1).toString());
                        break;
                    }
                }
                params.add(param);
            }

            //Preconditions required to be a string of ANDS
            Term ctx = op.getContext();

            Expression preconds = getExpression(ctx, paramsWithTypes);

            Expression effects = new Expression();

            //Deterministic Case
            if(op.getBody().getBodyNext() == null){
                effects = getExpression(op.getBody().getBodyTerm(), paramsWithTypes);
            } else {
                effects.setConnector(Connector.ASSIGN);
                effects.setSymbol(new Symbol(SymbolType.FUNCTOR, "oneof"));
                PlanBody curr = op.getBody();
                while(curr != null){
                    effects.addChild(getExpression(curr.getBodyTerm(), paramsWithTypes));
                    curr = curr.getBodyNext();
                }
            }


            ParsedAction action = new ParsedAction(
                    new TypedSymbol<>(SymbolType.ACTION, op.getTrigger().getLiteral().getFunctor()),
                    params,
                    preconds,
                    effects
            );
            domain.addAction(action);
        }

        //Goal
        Expression goal = new Expression();
        for(Term pred : nd.goalState){
            goal.addChild(getExpression(pred, Collections.emptyMap()));
        }
        System.out.println("GOALL " + goal);
        problem.setGoal(goal);

        //init
        for(Literal bel : nd.initialBeliefs) {
            Expression init = new Expression(Connector.ATOM);
            init.setSymbol(new Symbol(SymbolType.PREDICATE, bel.getFunctor()));
            if(bel.hasTerm()){
                for (Term term : bel.getTerms()){
                    init.addArgument(new Symbol(SymbolType.CONSTANT, term.toString()));
                }
                if(!this.predicates.containsKey(bel.getFunctor())){
                    List<String> types = new ArrayList<>();
                    for (Term term : bel.getTerms()){
                        for(TypedSymbol object : problem.getObjects()){
                            if(term.toString().equals(object.getValue().toString())){
                                types.add(object.getTypes().get(0).toString());
                                break;
                            }
                        }
                    }
                    predicates.put(bel.getFunctor(), types);
                }
            } else {
                if(!this.predicates.containsKey(bel.getFunctor())){
                    predicates.put(bel.getFunctor(), Collections.emptyList());
                }
            }

            problem.addInitialFact(init);
        }

        //Adds Predicates
        //Note: Requires predicates to be defined.
        //Example: predicate(clean, cell)
        //         predicate(linked, cell, cell)
        // -> The annotation denotes that the term in position 0 is of type cell
        for(String predicate : this.predicates.keySet()){
            NamedTypedList pred = new NamedTypedList(new Symbol<>(SymbolType.PREDICATE, predicate));
            for(int i=0; i<this.predicates.get(predicate).size(); i++){
                TypedSymbol s = new TypedSymbol(SymbolType.VARIABLE, "?v"+i);
                s.addType(new Symbol(SymbolType.TYPE, this.predicates.get(predicate).get(i)));
                pred.add(s);
            }
            domain.addPredicate(pred);
        }

        String domainOut = domain.toString().replace(":typing", ":typing :non-deterministic").replace("(assign", "(oneof").replace("(None )", "(and)");

        //Removing Auto GeneratedTasks
        int start = domainOut.indexOf("(:task");
        int end = domainOut.indexOf("(:action");
        System.out.println("S" + start + " E" + end);
        String del = "";
        char[] outCharArray = domainOut.toCharArray();

        for(int i=start; i < end; i++){
            del += outCharArray[i];
        }
        domainOut = domainOut.replace(del, "");
        domainOut = domainOut.replaceAll("~", "strong_negate_");

        String problemOut = "(define (problem p1)\n(:domain d1)\n(:objects\n";
        for(TypedSymbol t : problem.getObjects()){
            problemOut += t.toString()+"\n";
        }
        problemOut+= ")\n(:init\n";
        for(Expression e : problem.getInit()){
            problemOut += e+"\n";
        }
        problemOut+=")\n(:goal\n";
        problemOut+=problem.getGoal().toString();
        problemOut+="\n))";
        problemOut = problemOut.replaceAll("~", "_strong_negate_");



        //Creating Domain File
        try{
            System.out.println(domainOut);
            File domainFile = new File("domain.pddl");
            domainFile.createNewFile();
            FileWriter writer = new FileWriter("domain.pddl");
            writer.write(domainOut);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Creating Problem File
        try{
            System.out.println(problemOut);
            File problemFile = new File("task.pddl");
            problemFile.createNewFile();
            FileWriter writer = new FileWriter("task.pddl");
            writer.write(problemOut);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     *
     * @param term
     * @param vars List of the variables in the expression and their corresponding types
     * @return
     * @throws JasonException
     */
    private Expression getExpression(Term term, Map<String, String> vars) throws JasonException {
        if(term instanceof LogExpr){
            if(((LogExpr) term).getOp().equals(LogExpr.LogicalOp.not)){
                Expression exp = new Expression(Connector.NOT);
                exp.addChild(getExpression(((LogExpr) term).getLHS(), vars));
                //System.out.println(term + " *** " );

                return exp;
            }
            else if(((LogExpr) term).getOp().equals(LogExpr.LogicalOp.and)){
                Expression exp = new Expression();
                exp.addChild(getExpression(((LogExpr) term).getLHS(), vars));
                exp.addChild(getExpression(((LogExpr) term).getRHS(), vars));
                //System.out.println(term + " *** " );

                return exp;
            }
        } else if(term instanceof RelExpr){
            Expression exp = getRelativeExpression((RelExpr) term, vars);
            //System.out.println(term + " *** " + exp );

            return exp;
        } else if(term instanceof Literal){
            Expression exp = new Expression(Connector.ATOM);
            exp.setSymbol(new Symbol(SymbolType.PREDICATE, ((Literal) term).getFunctor()));
            if(((Literal) term).hasTerm()){
                boolean newPred = this.predicates.containsKey(((Literal) term).getFunctor());
                for(Term arg : ((Literal) term).getTerms()){
                    if(newPred){
                        exp.addArgument(getCorrectSymbol(arg.toString(), vars, null));
                    } else {
                        exp.addArgument(getCorrectSymbol(arg.toString(), vars, ((Literal) term).getFunctor()));
                    }
                }
            }

            //System.out.println(term + " *** " );

            return exp;
        }
        throw new JasonException("Creation of Expression failed for " + term + "of type: " + term.getClass().getTypeName());
    }

    private Expression getRelativeExpression(RelExpr expr, Map<String, String> vars){
        Expression exp = new Expression<>();
        switch(expr.getOp()){
            case none:
                break;
            case gt:
                exp.setConnector(Connector.GREATER_COMPARISON);
                break;
            case gte:
                exp.setConnector(Connector.GREATER_OR_EQUAL_COMPARISON);
                break;
            case lt:
                exp.setConnector(Connector.LESS_COMPARISON);
                break;
            case lte:
                exp.setConnector(Connector.LESS_OR_EQUAL_COMPARISON);
                break;
            case eq:
                exp.setConnector(Connector.EQUAL_COMPARISON);
                break;
            case dif:
                exp.setConnector(Connector.NOT);
                Expression subExp = new Expression(Connector.EQUAL_COMPARISON);
                Expression left = new Expression(Connector.ATOM);
                left.setSymbol(getCorrectSymbol(expr.getLHS().toString(), vars, null));
                Expression right = new Expression(Connector.ATOM);
                right.setSymbol(getCorrectSymbol(expr.getRHS().toString(), vars, null));
                subExp.addChild(left);
                subExp.addChild(right);
                exp.addChild(subExp);
                break;
        }
        if(expr.getOp() != RelExpr.RelationalOp.dif){
            Expression left = new Expression(Connector.ATOM);
            left.setSymbol(getCorrectSymbol(expr.getLHS().toString(), vars, null));
            Expression right = new Expression(Connector.ATOM);
            right.setSymbol(getCorrectSymbol(expr.getRHS().toString(), vars, null));
            exp.addChild(left);
            exp.addChild(right);

        }
        return exp;
    }

    /**
     *
     * @param name the name of the var/const
     * @param vars A map of the variables used in the expression to their types
     * @param predicateName The name of the associated predicate or null
     * @return
     */
    private Symbol getCorrectSymbol(String name, Map<String, String> vars, String predicateName){
        SymbolType s;

        //Variable is denoted with a ?
        if(vars.keySet().contains(name)){
            s = SymbolType.VARIABLE;
            if(predicateName != null){
                this.predicates.computeIfAbsent(predicateName, k -> new ArrayList<>());
                System.out.println("BEFORE PRED: " + predicateName + " | " + predicates);

                List<String> temp = this.predicates.get(predicateName);
                temp.add(vars.get(name));
                this.predicates.put(predicateName, temp);
                System.out.println("AFTER PRED: " + predicateName + " | " + predicates);
            }
            return new Symbol(s, "?"+name);

            //Constant
        } else {
            s = SymbolType.CONSTANT;
            if(predicateName != null) {
                this.predicates.computeIfAbsent(predicateName, k -> new ArrayList<>());
                System.out.println("BEFORE PRED: " + predicateName + " | " + predicates);
                List<String> temp = this.predicates.get(predicateName);
                temp.add(this.problem.getObjects().stream().filter(o -> o.getValue().equals(name)).toList().get(0).getTypes().get(0).toString());
                this.predicates.put(predicateName, temp);
                System.out.println("AFTER PRED: " + predicateName + " | " + predicates);

            }
            return new Symbol(s, name);

        }
    }





}
