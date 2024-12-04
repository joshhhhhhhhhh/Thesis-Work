package nd;

import jason.asSemantics.InternalAction;
import jason.asSyntax.Literal;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;
import jason.environment.Environment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class ErraticVacuumEnv extends Environment {

    int cellNum;
    int pos;
    int[] cellStates;
    Random r;

    private static final Logger logger = Logger.getLogger(InternalAction.class.getName());

    @Override
    public void init(String[] args) {
        this.cellNum = Integer.parseInt(args[0]);
        this.pos = 0;
        this.cellStates = new int[cellNum];
        for(int i=0; i<this.cellNum; i++){
            this.cellStates[i] = 1;
        }
        updatePercepts();
    }

    @Override
    public boolean executeAction(String agName, Structure act) {
        this.r = new Random();
        if(act.getFunctor().equals("suck")){
            if(cellStates[pos] == 1){
                cellStates[pos] = 0;
                //addPercept(Literal.parseLiteral("clean(" + pos + ")"));
                //removePercept(Literal.parseLiteral("clean(" + pos + ")"));
                if(pos == 0){
                    if(r.nextDouble() < 0.50){
                        cellStates[pos+1] = 0;
                        //addPercept(Literal.parseLiteral("clean(" + (pos+1) + ")"));
                        //removePercept(Literal.parseLiteral("dirty(" + (pos+1) + ")"));
                    }
                } else if (pos == cellNum-1){
                    if(r.nextDouble() < 0.50){
                        cellStates[pos-1] = 0;
                        //addPercept(Literal.parseLiteral("clean(" + (pos-1) + ")"));
                        //removePercept(Literal.parseLiteral("dirty(" + (pos-1) + ")"));
                    }
                } else {
                    if(r.nextDouble() < 0.50){
                        cellStates[pos+1] = 0;
                        cellStates[pos-1] = 0;
                        //addPercept(Literal.parseLiteral("clean(" + (pos+1) + ")"));
                        //removePercept(Literal.parseLiteral("dirty(" + (pos+1) + ")"));
                        //addPercept(Literal.parseLiteral("clean(" + (pos-1) + ")"));
                        //removePercept(Literal.parseLiteral("dirty(" + (pos-1) + ")"));
                    }
                }

            } else {
                if(r.nextDouble() < 0.50){
                    cellStates[pos] = 1;
                    //addPercept(Literal.parseLiteral("dirty(" + pos + ")"));
                    //removePercept(Literal.parseLiteral("clean(" + pos + ")"));
                }
            }
        } else if(act.getFunctor().equals("left")){
            if(pos!=0){
                //addPercept(Literal.parseLiteral("pos(" + (pos-1) + ")"));
                //removePercept(Literal.parseLiteral("clean(" + pos + ")"));
                pos-=1;
            }
        } else if(act.getFunctor().equals("right")){
            if(pos!=cellNum-1){
                //addPercept(Literal.parseLiteral("pos(" + (pos-1) + ")"));
                //(Literal.parseLiteral("clean(" + pos + ")"));
                pos+=1;
            }
        } else {
            System.out.println("WRONG ACTION: " + act.getFunctor());
        }
        //logger.info("PERCEPTS BEFORE ACTION: " + getPercepts(agName));
        logger.info(act.getFunctor() + " ACTION TAKEN");

        updatePercepts();
        /*
        try{
            Thread.sleep(200);
        } catch (Exception e) {}
        informAgsEnvironmentChanged();
        */
        return true;
    }

    public void updatePercepts(){
        clearPercepts();

        //adding preds
        addPercept(Literal.parseLiteral("predicate(clean, cell)"));
        addPercept(Literal.parseLiteral("predicate(dirty, cell)"));
        addPercept(Literal.parseLiteral("predicate(pos, cell)"));
        addPercept(Literal.parseLiteral("predicate(linked, cell, cell)"));



        Literal posLit = Literal.parseLiteral("pos(c" + this.pos + ")");
        List<Literal> perceptLog = new ArrayList<>();
        addPercept(posLit);
        perceptLog.add(posLit);
        Literal[] cleans = new Literal[cellNum];

        for (int i=0; i<cellNum;i++){
            if(i!=0)
                addPercept(Literal.parseLiteral("linked(c" + (i-1) + ", c" + i + ")"));

            addPercept(Literal.parseLiteral("object(cell, c" + i + ")"));
            cleans[i] = Literal.parseLiteral("clean(c" + i + ")");

            //clean
            if(cellStates[i] == 0){
                Literal cellLit = Literal.parseLiteral("clean(c" + i + ")");
                addPercept(cellLit);
                perceptLog.add(cellLit);
            } //dirty
            else {
                Literal cellLit = Literal.parseLiteral("dirty(c" + i + ")");
                addPercept(cellLit);
                perceptLog.add(cellLit);
            }
        }
        addPercept(Literal.parseLiteral("desires(" + Arrays.toString(cleans) + ")"));
        logger.info("CURRENT PERCEPTS: " + perceptLog);
    }

}
