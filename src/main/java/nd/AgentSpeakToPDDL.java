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

    public AgentSpeakToPDDL(){}
    public static void generatePDDL(NonDeterministicValues nd) {
        try {
            generateDomain(nd);
        } catch (JasonException e) {
            System.out.println("Could Not Parse Domain: " + e.getMessage());
        }
        try {
            generateProblem(nd);
        } catch(JasonException e){
            System.out.println("Could Not Parse Problem: " + e.getMessage());
        }
    }

    private static void generateDomain(NonDeterministicValues nd) throws JasonException {
        ParsedDomain domain = new DefaultParsedProblem(new Symbol<>(SymbolType.DOMAIN, "d1"));

        domain.addRequirement(RequireKey.EQUALITY);
        domain.addRequirement(RequireKey.STRIPS);
        domain.addRequirement(RequireKey.TYPING);
        //TODO: Add :non-deterministic requirement manually

        //Adds the types
        for(Literal type : nd.objects.keySet()){
            domain.addType(new TypedSymbol<>(new Symbol<>(SymbolType.TYPE, type.toString())));
        }

        //Adds Predicates
        //Note: Requires Initial Beliefs to be annoted with types
        //Example: pos(X)[type(0, cell)]
        // -> The annotation denotes that the term in position 0 is of type cell
        System.out.println(nd.initialBeliefs);
        for(Literal bel : nd.initialBeliefs){
            NamedTypedList pred = new NamedTypedList(new Symbol<>(SymbolType.PREDICATE, bel.getFunctor()));
            for(int i=0; i<bel.getTerms().size(); i++){
                TypedSymbol s = new TypedSymbol(SymbolType.VARIABLE, "?v"+i);
                System.out.println(bel);
                for(Term t : bel.getAnnots().getAsList()){
                    Literal lit = (Literal)t;
                    if(Integer.parseInt(lit.getTerm(0).toString()) == i){
                        s.addType(new Symbol(SymbolType.TYPE, lit.getTerm(1)));
                    }
                }
                //s.addType(new Symbol(SymbolType.TYPE, bel.getAnnots().get(i)));
                pred.add(s);
            }
            System.out.println("PRED: " + pred);
            domain.addPredicate(pred);
        }

        //Adds Actions :)
        for(Plan op : nd.operators){
            List<String> actionVariables = op.getTrigger().getLiteral().getTerms().stream().map(Object::toString).toList();
            List<Term> types = op.getLabel().getAnnots().getAsList().stream().filter(t->t.toString().contains("type(")).toList();


            List<TypedSymbol<String>> params = new ArrayList<>();
            for(String var : actionVariables){
                TypedSymbol param = new TypedSymbol(SymbolType.VARIABLE, "?"+var);
                for(Term t : types){
                    Literal lit = (Literal)t;
                    if(lit.getTerm(0).toString().equals(var)){
                        param.addType(new Symbol(SymbolType.TYPE, lit.getTerm(1).toString()));
                        break;
                    }
                }
                params.add(param);
            }

            //Preconditions required to be a string of ANDS
            Term ctx = op.getContext();

            Expression preconds = getExpression(ctx, actionVariables);

            Expression effects = new Expression();

            //Deterministic Case
            if(op.getBody().getBodyNext() == null){
                effects = getExpression(op.getBody().getBodyTerm(), actionVariables);
            } else {
                effects.setConnector(Connector.ASSIGN);
                effects.setSymbol(new Symbol(SymbolType.FUNCTOR, "oneof"));
                PlanBody curr = op.getBody();
                while(curr != null){
                    effects.addChild(getExpression(curr.getBodyTerm(), actionVariables));
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

        String out = domain.toString().replace(":typing", ":typing :non-deterministic").replace("(assign", "(oneof").replace("(None )", "(and)");

        //Removing Auto GeneratedTasks
        int start = out.indexOf("(:task");
        int end = out.indexOf("(:action");
        System.out.println("S" + start + " E" + end);
        String del = "";
        char[] outCharArray = out.toCharArray();

        for(int i=start; i < end; i++){
            del += outCharArray[i];
        }
        out = out.replace(del, "");

        //System.out.println("DOMAIN: " + domain.toString());
        try{
            File domainFile = new File("domain.pddl");
            domainFile.createNewFile();
            FileWriter writer = new FileWriter("domain.pddl");
            writer.write(out);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void generateProblem(NonDeterministicValues nd) throws JasonException {
        ParsedProblem problem = new DefaultParsedProblem(new Symbol<String>(SymbolType.DOMAIN, "domain"));
        problem.setDomainName(new Symbol<>(SymbolType.DOMAIN, "domain"));
        //Goal
        Expression goal = new Expression();
        for(Term pred : nd.goalState){
            goal.addChild(getExpression(pred, Collections.emptyList()));
        }
        System.out.println("GOALL " + goal);
        problem.setGoal(goal);

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

        //init
        for(Literal bel : nd.initialBeliefs) {
            Expression init = new Expression(Connector.ATOM);
            init.setSymbol(new Symbol(SymbolType.PREDICATE, bel.getFunctor()));
            for (Term term : bel.getTerms()){
                init.addArgument(new Symbol(SymbolType.VARIABLE, "?" + term.toString()));
            }
            problem.addInitialFact(init);
        }

        String out = "(define (problem p1)\n(:domain d1)\n(:objects\n";
        for(TypedSymbol t : problem.getObjects()){
            out += t.toString()+"\n";
        }
        out+= ")\n(:init\n";
        for(Expression e : problem.getInit()){
            out += e+"\n";
        }
        out+=")\n(:goal\n";
        out+=problem.getGoal().toString();
        out+="\n))";
        System.out.println("PROBLEM: " + out);

        try{
            File domainFile = new File("task.pddl");
            domainFile.createNewFile();
            FileWriter writer = new FileWriter("task.pddl");
            writer.write(out);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private static Expression getExpression(Term term, List<String> vars) throws JasonException {
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
                for(Term arg : ((Literal) term).getTerms()){
                    exp.addArgument(getCorrectSymbol(arg.toString(), vars));
                }
            }

            //System.out.println(term + " *** " );

            return exp;
        }
        throw new JasonException("Creation of Expression failed for " + term + "of type: " + term.getClass().getTypeName());
    }

    private static Expression getRelativeExpression(RelExpr expr, List<String> vars){
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
                left.setSymbol(getCorrectSymbol(expr.getLHS().toString(), vars));
                Expression right = new Expression(Connector.ATOM);
                right.setSymbol(getCorrectSymbol(expr.getRHS().toString(), vars));
                subExp.addChild(left);
                subExp.addChild(right);
                exp.addChild(subExp);
                break;
        }
        if(expr.getOp() != RelExpr.RelationalOp.dif){
            Expression left = new Expression(Connector.ATOM);
            left.setSymbol(getCorrectSymbol(expr.getLHS().toString(), vars));
            Expression right = new Expression(Connector.ATOM);
            right.setSymbol(getCorrectSymbol(expr.getRHS().toString(), vars));
            exp.addChild(left);
            exp.addChild(right);

        }
        return exp;
    }

    private static Symbol getCorrectSymbol(String name, List<String> vars){
        SymbolType s;

        //Variable is denoted with a ?
        if(vars.contains(name)){
            s = SymbolType.VARIABLE;
            return new Symbol(s, "?"+name);

            //Constant
        } else {
            s = SymbolType.CONSTANT;
            return new Symbol(s, name);

        }
    }





}
