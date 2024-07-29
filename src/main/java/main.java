import com.opencsv.CSVWriter;
import core.search.nondeterministic.AndOrSearch;
import core.search.nondeterministic.NondeterministicProblem;
import core.search.nondeterministic.Plan;
import jason.RevisionFailedException;
import jason.asSyntax.*;
import jason.asSyntax.parser.ParseException;
import org.apache.logging.log4j.core.pattern.LiteralPatternConverter;
import org.soton.peleus.act.planner.PlannerConverter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


public class main {



    private static void testErraticVacuum() throws ParseException {
        Set<Literal> init1 = Set.of(Literal.parseLiteral("pos(0)"),Literal.parseLiteral("dirty(1)"), Literal.parseLiteral("dirty(0)"));
        Set<LogicalFormula> goals = Set.of(ASSyntax.parseFormula("clean(0)"),ASSyntax.parseFormula("clean(1)"), ASSyntax.parseFormula("not dirty(0)"), ASSyntax.parseFormula("not dirty(1)"));
        //Set<String> init2D = Set.of("pos(0,0)", "dirty(0,0)", "dirty(0,1)", "dirty(1,0)", "dirty(1,1)");
        //Generator.generate(p1, init, true);

        Map<VarTerm, List<Term>> map = new HashMap<VarTerm, List<Term>>() {{
            put(new VarTerm("X"), Arrays.asList(new NumberTermImpl(0),new NumberTermImpl(1)));
            put(new VarTerm("Y"), Arrays.asList(new NumberTermImpl(0),new NumberTermImpl(1)));
        }};

        //System.out.println(init1.toString().substring(1, init1.toString().length()-1));

        List<jason.asSyntax.Plan> operators = new ArrayList<>();

        operators.add(jason.asSyntax.Plan.parse("+!suck : pos(X) & dirty(X) & X \\== Y &  not pos(Y) <- (not dirty(X)) & clean(X) & (not dirty(Y)) & clean(Y); (not dirty(X)) & clean(X)."));
        operators.add(jason.asSyntax.Plan.parse("+!suck : pos(X) & clean(X) & X \\== Y & not pos(Y) <- dirty(X) & (not clean(X)); None."));
        operators.add(jason.asSyntax.Plan.parse("+!right : pos(0) <- pos(1) & not pos(0)."));
        operators.add(jason.asSyntax.Plan.parse("+!left : pos(1) <- pos(0) & not pos(1)."));

        List<Literal> actions = new ArrayList<>();

        actions.add(Literal.parseLiteral("suck(X, Y)"));
        actions.add(Literal.parseLiteral("right(X)"));
        actions.add(Literal.parseLiteral("left(X)"));

        NonDeterministicValues vals = new NonDeterministicValues(map, goals, actions);

        System.out.println(vals.getActions(null));


        //System.out.println(vals.results(operators).results(init1, "left"));
        //System.out.println(vals.testGoalFunction(Set.of(Literal.parseLiteral("pos(0)"),Literal.parseLiteral("clean(1)"), Literal.parseLiteral("clean(0)"))));

        System.out.println("---------------------------");
        double start = System.currentTimeMillis();
        NondeterministicProblem problem = new NondeterministicProblem(
                init1,
                vals::getActions,
                vals.results(operators),
                vals::testGoalFunction);
        AndOrSearch<List<Literal>, Literal> search = new AndOrSearch<>();
        Optional plan = search.search(problem);
        Generator_V2 g = new Generator_V2();
        PlanLibrary lib = g.generate((Plan) plan.get(), init1);
        System.out.println(lib);
        Reducer.reduce(lib);
        System.out.println(lib);

    }

    public static void testManyCells(int n, CSVWriter writer) throws ParseException, RevisionFailedException {
        Set<Literal> init = new HashSet<>();
        init.add(Literal.parseLiteral("pos(0)"));
        Set<LogicalFormula> goals = new HashSet<>();
        List<Term> paramVals = new ArrayList();

        for(int i = 0; i<n; i++){
            init.add(Literal.parseLiteral("dirty(" + i + ")"));
            goals.add(ASSyntax.parseFormula("clean(" + i + ")"));
            goals.add(ASSyntax.parseFormula("not dirty(" + i + ")"));
            paramVals.add(new NumberTermImpl(i));
        }

        Map<VarTerm, List<Term>> map = new HashMap<VarTerm, List<Term>>() {{
            put(new VarTerm("X"), paramVals);
            //put(new VarTerm("Y"), paramVals);
        }};

        //System.out.println(init1.toString().substring(1, init1.toString().length()-1));

        List<jason.asSyntax.Plan> operators = new ArrayList<>();

        operators.add(jason.asSyntax.Plan.parse("+!suck : pos(X) & dirty(X) & X == " + (n-1) + " <- (not dirty(X)) & clean(X) & (not dirty(X-1)) & clean(X-1); (not dirty(X)) & clean(X)."));
        operators.add(jason.asSyntax.Plan.parse("+!suck : pos(X) & dirty(X) & X == 0 <- (not dirty(X)) & clean(X) & (not dirty(X+1)) & clean(X+1); (not dirty(X)) & clean(X)."));
        operators.add(jason.asSyntax.Plan.parse("+!suck : pos(X) & dirty(X) & X \\== 0 & X \\== " + (n-1) + " <- (not dirty(X)) & clean(X) & (not dirty(X+1)) & clean(X+1) & (not dirty(X-1)) & clean(X-1); (not dirty(X)) & clean(X)."));
        operators.add(jason.asSyntax.Plan.parse("+!suck : pos(X) & clean(X) <- dirty(X) & (not clean(X)); None."));
        operators.add(jason.asSyntax.Plan.parse("+!right : pos(X) & X \\== " + (n-1) + " <- pos(X+1) & not pos(X)."));
        operators.add(jason.asSyntax.Plan.parse("+!left : pos(X) & X \\== 0 <- pos(X-1) & not pos(X)."));

        List<Literal> actions = new ArrayList<>();

        actions.add(Literal.parseLiteral("suck(X)"));
        actions.add(Literal.parseLiteral("right(X)"));
        actions.add(Literal.parseLiteral("left(X)"));

        double start = System.currentTimeMillis();

        NonDeterministicValues vals = new NonDeterministicValues(map, goals, actions);

        //System.out.println(vals.getActions(null));


        //System.out.println(vals.results(operators).results(init, Literal.parseLiteral("suck(0)")));


        //System.out.println("---------------------------");
        double afterSetup = System.currentTimeMillis();

        NondeterministicProblem problem = new NondeterministicProblem(
                init,
                vals::getActions,
                vals.results(operators),
                vals::testGoalFunction);

        double afterCreation = System.currentTimeMillis();

        AndOrSearch<List<Literal>, Literal> search = new AndOrSearch<>();
        Optional plan = search.search(problem);

        double afterSearch = System.currentTimeMillis();

        Generator_V2 g = new Generator_V2();
        PlanLibrary lib = g.generate((Plan) plan.get(), init);

        double end = System.currentTimeMillis();

        //System.out.println(lib);
        int libSize = lib.size();
        Reducer.reduce(lib);
        //System.out.println(lib);
        //[] data = { String.valueOf(n), String.valueOf(libSize),String.valueOf(afterSetup-start) , String.valueOf(afterCreation-afterSetup) , String.valueOf(afterSearch-afterCreation) , String.valueOf(end-afterSearch) };
        String[] data = { String.valueOf(n), String.valueOf(libSize), String.valueOf(lib.size())};

        writer.writeNext(data);


    }

    private static void testTables() throws ParseException, RevisionFailedException {

        double start = System.currentTimeMillis();

        Set<Literal> init2 = Set.of(Literal.parseLiteral("clear(\"b1\")"),Literal.parseLiteral("clear(\"b3\")"), Literal.parseLiteral("clear(\"table\")"), Literal.parseLiteral("on(\"b1\", \"b2\")"), Literal.parseLiteral("on(\"b2\", \"table\")"), Literal.parseLiteral("on(\"b3\", \"table\")"));
        Set<LogicalFormula> goals2 = Set.of(ASSyntax.parseFormula("on(\"b3\", \"table\")"),ASSyntax.parseFormula("on(\"b2\", \"b3\")"), ASSyntax.parseFormula("on(\"b1\", \"b2\")"));

        List<jason.asSyntax.Plan> operators2 = new ArrayList<>();

        operators2.add(jason.asSyntax.Plan.parse("+!moveToTable : Block \\== From & Block \\== \"table\" & From \\== \"table\" & clear(Block) & on(Block, From) <- on(Block, \"table\") & clear(From) & not on(Block, From)."));
        operators2.add(jason.asSyntax.Plan.parse("+!move : Block \\== From & Block \\== To & From \\== To & To \\== \"table\" & clear(Block) & on(Block, From) & clear(To) <- on(Block, To) & clear(From) & (not on(Block, From)) & not clear(To)."));

        Map<VarTerm, List<Term>> map2 = new HashMap<VarTerm, List<Term>>() {{
            put(new VarTerm("Block"), Arrays.asList(new StringTermImpl("b1"),new StringTermImpl("b2"),new StringTermImpl("b3"),new StringTermImpl("table")));
            put(new VarTerm("From"), Arrays.asList(new StringTermImpl("b1"),new StringTermImpl("b2"),new StringTermImpl("b3"),new StringTermImpl("table")));
            put(new VarTerm("To"), Arrays.asList(new StringTermImpl("b1"),new StringTermImpl("b2"),new StringTermImpl("b3"),new StringTermImpl("table")));
        }};

        List<Literal> actions = new ArrayList<>();
        actions.add(Literal.parseLiteral("moveToTable(Block, From)"));
        actions.add(Literal.parseLiteral("move(Block, From, To)"));

        NonDeterministicValues vals2 = new NonDeterministicValues(map2, goals2, actions);
        //System.out.println(operators2);

        //System.out.println(vals2.results(operators2).results(init2, "move"));
        double afterSetup = System.currentTimeMillis();

        System.out.println("---------------------------");
        NondeterministicProblem problem = new NondeterministicProblem(
                init2,
                vals2::getActions,
                vals2.results(operators2),
                vals2::testGoalFunction);
        double afterCreation = System.currentTimeMillis();

        AndOrSearch<List<Literal>, Literal> search = new AndOrSearch<>();
        Optional plan = search.search(problem);
        double afterSearch = System.currentTimeMillis();

        Generator_V2 g = new Generator_V2();
        PlanLibrary lib = g.generate((Plan) plan.get(), init2);
        double end = System.currentTimeMillis();

        System.out.println(String.valueOf(afterSetup-start) + "," + String.valueOf(afterCreation-afterSetup) + "," + String.valueOf(afterSearch-afterCreation) + "," + String.valueOf(end-afterSearch));

        System.out.println(lib);
    }

    public static void testTimings(String[] args) {
        File file = new File("timingsWithLibSizeBeforeAndAfterReduction.csv");
        try {
            FileWriter outputFile = new FileWriter(file);
            CSVWriter writer = new CSVWriter(outputFile);
            //String[] header = {"Number of Cells", "Number of Generated Plans","Setup", "Problem Creation", "Search", "Plan Generation"};
            String[] header = {"Number of Cells", "Number of Generated Plans","Number of Plans after reduction"};
            writer.writeNext(header);

            int[] rounds = {2,3,4,5,6,7,8,9,10,15,20,25,30,35,40,45,50,60,70,80,90,100,120,140};//,160,180,200,250,300,350,400,450,500,550,600,650,700,750,800,850,900,950,1000};
            for(int i=0;i<121;i++){
                testManyCells(i, writer);
                System.out.println(i);
            }
            writer.close();
        } catch (IOException | ParseException | RevisionFailedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws ParseException, RevisionFailedException {

        //testTables();
        //testManyCells(3);
        //testErraticVacuum();


    }
}
