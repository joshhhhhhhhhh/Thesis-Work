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
import java.util.logging.Logger;

public class BlocksworldEnvironment extends Environment {


    private static final Logger logger = Logger.getLogger(InternalAction.class.getName());

    List<Block> blocks;
    Block table;
    int NUM_BLOCKS = 10;
    Block holding;


    @Override
    public void init(String[] args) {
        this.blocks = new ArrayList<>();
        holding = null;
        for(int i=0; i<NUM_BLOCKS; i++){
            this.blocks.add(new Block("block"+i));
        }
        this.table = new Block("table", true);

        blocks.get(0).setBelow(blocks.get(4));
        blocks.get(1).setBelow(blocks.get(8));
        blocks.get(2).setBelow(this.table);
        blocks.get(3).setBelow(blocks.get(5));
        blocks.get(4).setBelow(blocks.get(1));
        blocks.get(5).setBelow(this.table);
        blocks.get(6).setBelow(blocks.get(7));
        blocks.get(7).setBelow(blocks.get(0));
        blocks.get(8).setBelow(blocks.get(2));
        blocks.get(9).setBelow(this.table);

        updatePercepts();
    }

    @Override
    public boolean executeAction(String agName, Structure act) {
        if(act.getFunctor().equals("pickup")){
            if(act.getTerms().size() == 1){
                Block block = this.blocks.get(Integer.parseInt(act.getTerm(0).toString().replace("block", "")));
                if(block.above()!=null){
                    System.out.println("ERROR RUNNING " + act);
                    return false;
                }

                Random r = new Random();
                if(r.nextDouble() > 0.5){
                    block.setBelow(null);
                    holding = block;
                } else {
                    block.setBelow(this.table);
                }
            } else {
                System.out.println("ERROR RUNNING " + act);
                return false;
            }
        } else if(act.getFunctor().equals("pickupfromtable")){
            if(act.getTerms().size() == 1){
                Block block = this.blocks.get(Integer.parseInt(act.getTerm(0).toString().replace("block", "")));
                if(block.above()!=null){
                    System.out.println("ERROR RUNNING " + act);
                    return false;
                }

                Random r = new Random();
                if(r.nextDouble() > 0.5){
                    block.setBelow(null);
                    holding = block;
                }
            } else {
                System.out.println("ERROR RUNNING " + act);
                return false;
            }
        } else if(act.getFunctor().equals("putonblock")){
            if(act.getTerms().size() == 2){
                Block block1 = this.blocks.get(Integer.parseInt(act.getTerm(0).toString().replace("block", "")));
                Block block2 = this.blocks.get(Integer.parseInt(act.getTerm(1).toString().replace("block", "")));

                if(block2.above()!=null || !this.holding.equals(block1) || block1.equals(block2)){
                    System.out.println("ERROR RUNNING " + act);
                    return false;
                }

                Random r = new Random();
                holding = null;
                if(r.nextDouble() > 0.5){
                    block1.setBelow(this.table);
                } else {
                    block1.setBelow(block2);
                }
            } else {
                System.out.println("ERROR RUNNING " + act);
                return false;
            }
        } else if(act.getFunctor().equals("putdown")){
            if(act.getTerms().size() == 1){
                Block block = this.blocks.get(Integer.parseInt(act.getTerm(0).toString().replace("block", "")));
                if(!this.holding.equals(block)){
                    System.out.println("ERROR RUNNING " + act);
                    return false;
                }

                Random r = new Random();
                if(r.nextDouble() > 0.5){
                    block.setBelow(table);
                    holding = null;
                }
            } else {
                System.out.println("ERROR RUNNING " + act);
                return false;
            }
        } else if(act.getFunctor().equals("picktower")){
            if(act.getTerms().size() == 1){
                Block block = this.blocks.get(Integer.parseInt(act.getTerm(0).toString().replace("block", "")));
                if(block.above()==null || block.above().above()!=null || holding!=null){
                    System.out.println("ERROR RUNNING " + act);
                    return false;
                }

                Random r = new Random();
                if(r.nextDouble() > 0.5){
                    block.setBelow(null);
                    holding = block;
                }
            } else {
                System.out.println("ERROR RUNNING " + act);
                return false;
            }

        } else if(act.getFunctor().equals("puttoweronblock")){

            if(act.getTerms().size() == 2){
                Block block1 = this.blocks.get(Integer.parseInt(act.getTerm(0).toString().replace("block", "")));
                Block block2 = this.blocks.get(Integer.parseInt(act.getTerm(1).toString().replace("block", "")));

                if(block2.above()!=null || !this.holding.equals(block1) || block1.equals(block2)){
                    System.out.println("ERROR RUNNING " + act);
                    return false;
                }

                Random r = new Random();

                holding = null;
                if(r.nextDouble() > 0.5){
                    block1.setBelow(this.table);
                    block1.above().setBelow(this.table);
                } else {
                    block1.setBelow(block2);
                }
            } else {
                System.out.println("ERROR RUNNING " + act);
                return false;
            }

        } else if(act.getFunctor().equals("puttowerdown")){
            if(act.getTerms().size() == 1){
                Block block = this.blocks.get(Integer.parseInt(act.getTerm(0).toString().replace("block", "")));
                if(block.above()==null || block.above().above()!=null || !holding.equals(block)){
                    System.out.println("ERROR RUNNING " + act);
                    return false;
                }

                block.setBelow(table);
                holding = null;

            } else {
                System.out.println("ERROR RUNNING " + act);
                return false;
            }
        }
        logger.info(act + " ACTION TAKEN");
        return updatePercepts();
    }

    public boolean updatePercepts() {

        clearPercepts();
        if(holding!=null){
            addPercept(Literal.parseLiteral("holding(" + holding.name + ")"));
            if(holding.above()==null){
                addPercept(Literal.parseLiteral("holdingone"));
            } else {
                addPercept(Literal.parseLiteral("holdingtwo"));
            }
        } else {
            addPercept(Literal.parseLiteral("emptyhand"));
        }



        for(int i=0; i<NUM_BLOCKS; i++){
            Block block = blocks.get(i);
            addPercept(Literal.parseLiteral("object(block, " + block.name + ")"));

            if(block.below()==null){
            } else if(block.below().isTable()){
                addPercept(Literal.parseLiteral("ontable(block" + i + ")"));
                if(block.above()==null){
                    addPercept(Literal.parseLiteral("clear(block" + i + ")"));
                }
            } else {
                addPercept(Literal.parseLiteral("on(block" + i + ", " + block.below().name + ")"));
                if(block.above()==null){
                    addPercept(Literal.parseLiteral("clear(block" + i + ")"));
                }
            }
        }

        Literal[] goal = new Literal[]{
                Literal.parseLiteral("emptyhand"),
                Literal.parseLiteral("on(block0, block9)"),
                Literal.parseLiteral("on(block1, block5)"),
                Literal.parseLiteral("on(block2, block7)"),
                Literal.parseLiteral("on(block3, block1)"),
                Literal.parseLiteral("ontable(block4)"),
                Literal.parseLiteral("ontable(block5)"),
                Literal.parseLiteral("on(block6, block4)"),
                Literal.parseLiteral("ontable(block7)"),
                Literal.parseLiteral("on(block8, block2)"),
                Literal.parseLiteral("on(block9, block3)"),
                Literal.parseLiteral("clear(block0)"),
                Literal.parseLiteral("clear(block6)"),
                Literal.parseLiteral("clear(block8)")
        };

        addPercept(Literal.parseLiteral("desires(" + Arrays.toString(goal) + ")"));

        boolean goalReached = true;
        for(Literal g : goal){
            if(!containsPercept(g)){
                logger.info("Goal not reached, missing belief: " + g.toString());
                goalReached = false;
            }
        }
        if(goalReached)
            logger.info("Goal reached!");
        return true;
    }



    private class Block {
        String name;
        Block above;
        Block below;
        boolean table;

        public Block(String name) {
            this.name = name;
            this.table = false;
            this.above = null;
            this.below = null;
        }

        public Block(String name, boolean table) {
            this(name);
            this.table = table;
        }

        public Block below() {
            return below;
        }

        public void setBelow(Block below) {

            if(this.below==null){
                this.below = below;
                this.below.setAbove(this);
            } else if(below==null){
                this.below.setAbove(null);
                this.below = null;
            } else {
                this.below.setAbove(null);
                this.below = below;
                this.below.setAbove(this);
            }
        }

        public Block above() {
            return above;
        }

        private void setAbove(Block above) {
            if(!table)
                this.above = above;
        }

        public boolean isTable() {
            return table;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Block block = (Block) o;
            return table == block.table && name.equals(block.name);
        }

    }

}
