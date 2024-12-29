package nd;

import jason.asSemantics.InternalAction;
import jason.asSyntax.Literal;
import jason.asSyntax.Structure;
import jason.environment.Environment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public class TireWorldEnvironment extends Environment {

    String vehicleLocation;
    List<String> spareLocations;
    String[] locations;
    List<String> roads = new ArrayList<>();
    boolean spare;
    boolean flat;
    int LOCATION_NUM = 17;
    private static final Logger logger = Logger.getLogger(InternalAction.class.getName());


    @Override
    public void init(String[] args) {
        locations = new String[LOCATION_NUM];
        spareLocations = List.of(new String[]{"n4", "n5", "n7", "n8", "n10", "n12", "n16"});
        for(int i=0; i<LOCATION_NUM;i++){
            locations[i] = "n"+i;
        }
        roads.add("n0 n12");
        roads.add("n0 n16");
        roads.add("n1 n2");
        roads.add("n1 n3");
        roads.add("n3 n4");
        roads.add("n3 n13");
        roads.add("n3 n14");
        roads.add("n5 n8");
        roads.add("n5 n10");
        roads.add("n5 n16");
        roads.add("n6 n14");
        roads.add("n7 n9");
        roads.add("n7 n13");
        roads.add("n8 n9");
        roads.add("n9 n12");
        roads.add("n9 n16");
        roads.add("n10 n12");
        roads.add("n10 n13");
        roads.add("n11 n16");
        roads.add("n12 n16");
        roads.add("n13 n15");
        roads.add("n14 n16");
        List<String> temp = new ArrayList<>();
        for (String road: roads) {
            temp.add(road.split(" ")[1] + " " + road.split(" ")[0]);
        }
        roads.addAll(temp);
        vehicleLocation = "n2";
        flat = false;
        spare = false;
        updatePercepts();
    }

    @Override
    public boolean executeAction(String agName, Structure act) {

        if(act.getFunctor().equals("movecar")){
            if(!act.hasTerm()){
                return false;
            }
            String from = act.getTerm(0).toString();
            String to = act.getTerm(1).toString();

            if(vehicleLocation.equals(from) && roads.contains(from + " " + to) && !flat){
                vehicleLocation = to;
                Random r = new Random();
                if(r.nextDouble() < 0.33){
                    flat = true;
                }
            } else {
                logger.info(act + " FAILED. FLAT: " + flat + " | ROAD CONTAINS " + from + " " + to + ": " + roads.contains(from + " " + to) + " | LOCATION IS " + from + ": " + vehicleLocation.equals(from) + " ROADS: " + roads);
                return false;
            }

        } else if(act.getFunctor().equals("loadtire")){
            if(spareLocations.contains(vehicleLocation)){
                spare = true;
                spareLocations.remove(vehicleLocation);
            }
        } else if(act.getFunctor().equals("changetire")){
            if(spare){
                Random r = new Random();
                if(r.nextDouble() < 0.5){
                    flat = false;
                    spare = false;
                }
            }
        } else {
            logger.info("INVALID ACTION TAKEN: " + act);
        }

        logger.info(act + " PERFORMED");
        updatePercepts();
        return true;
    }

    private void updatePercepts(){
        clearPercepts();
        addPercept(Literal.parseLiteral("vehicleat(" + vehicleLocation + ")"));

        if(flat){
            addPercept(Literal.parseLiteral("flattire"));
        } else {
            addPercept(Literal.parseLiteral("~flattire"));
        }
        if(spare){
            addPercept(Literal.parseLiteral("hasspare"));
        } else {
            addPercept(Literal.parseLiteral("~hasspare"));
        }

        for(String loc : locations){
            addPercept(Literal.parseLiteral("object(location, " + loc + ")"));
        }

        for(String loc : spareLocations){
            addPercept(Literal.parseLiteral("sparein(" + loc + ")"));
        }

        for(String road : roads){
            String[] splitRoads = road.split(" ");
            addPercept(Literal.parseLiteral("road(" + splitRoads[0] + ", " + splitRoads[1] + ")"));
        }
        Literal[] goal = new Literal[]{Literal.parseLiteral("vehicleat(n0)")};

        addPercept(Literal.parseLiteral("desires(" + Arrays.toString(goal) + ")"));
        logger.info("CURRENT LOCATION: " + vehicleLocation + " | HAS FLAT: " + flat + " | HAS SPARE: " + spare);
    }

}
