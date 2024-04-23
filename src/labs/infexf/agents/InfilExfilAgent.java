package src.labs.infexf.agents;

// SYSTEM IMPORTS
import edu.bu.labs.infexf.agents.SpecOpsAgent;
import edu.bu.labs.infexf.distance.DistanceMetric;
import edu.bu.labs.infexf.graph.Vertex;
import edu.bu.labs.infexf.graph.Path;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import java.util.Stack;

import edu.cwru.sepia.environment.model.state.State.StateView;


// JAVA PROJECT IMPORTS


public class InfilExfilAgent
    extends SpecOpsAgent
{

    public InfilExfilAgent(int playerNum)
    {
        super(playerNum);
    }

    // if you want to get attack-radius of an enemy, you can do so through the enemy unit's UnitView
    // Every unit is constructed from an xml schema for that unit's type.
    // We can lookup the "range" of the unit using the following line of code (assuming we know the id):
    //     int attackRadius = state.getUnit(enemyUnitID).getTemplateView().getRange();
    @Override
    public float getEdgeWeight(Vertex src,
                               Vertex dst,
                               StateView state)
    {
        float baseWeight = 1.0f; // Base weight for all moves ensures coonsitency
        float dangerWeight = 27.0f; // Additional weight for moving close to enemies
        

        // Check each enemy's position and increase the weight if within attack range
        for (Integer enemyUnitID : this.getOtherEnemyUnitIDs()) {
            float attackRadius = state.getUnit(enemyUnitID).getTemplateView().getRange();
            UnitView enemyUnit = state.getUnit(enemyUnitID);
            Vertex enemyPosition = new Vertex(enemyUnit.getXPosition(), enemyUnit.getYPosition());

            // Calculate Chebyshev distance 
            float chebyshevDistance = Math.max(Math.abs(dst.getXCoordinate() - enemyPosition.getXCoordinate()), Math.abs(dst.getYCoordinate() - enemyPosition.getYCoordinate()))-1;

            // If the destination - next move - is within the enemy's attack range, increase the weight
            if (chebyshevDistance <= attackRadius) {
                baseWeight += dangerWeight;
            }
        }

        // Ensure that the weight is not less than 1
        return Math.max(baseWeight, 1.0f);
    }

    @Override
    public boolean shouldReplacePlan(StateView state)
    {   
        //System.out.println("new plan");
        UnitView myUnit = state.getUnit(getMyUnitID());
        Stack<Vertex> plan = getCurrentPlan(); // Retrieve the current plan
        if (plan.isEmpty()) {
            System.out.println("no new plan needed");
            return false;
        }

        // Get the current position of the agent
        Vertex currentPosition = new Vertex(myUnit.getXPosition(), myUnit.getYPosition());

        // Check the path for any enemies that might have moved into it
        for (Vertex step : plan) {
            for (Integer enemyUnitID : this.getOtherEnemyUnitIDs()) {
                UnitView enemyUnit = state.getUnit(enemyUnitID);
                Vertex enemyPosition = new Vertex(enemyUnit.getXPosition(), enemyUnit.getYPosition());
                float attackRadius = (state.getUnit(enemyUnitID).getTemplateView().getRange())+1.0f;

                float chebyshevDistance = Math.max(Math.abs(step.getXCoordinate() - enemyPosition.getXCoordinate()), Math.abs(step.getYCoordinate() - enemyPosition.getYCoordinate()));
                System.out.println("Checking enemy with ID " + enemyUnitID + " at position " + enemyPosition);
                System.out.println("Chebyshev distance to enemy: " + chebyshevDistance + ", Attack radius: " + attackRadius);

                // If an enemy unit is within attack range of the path, the plan should be replaced
                if (chebyshevDistance <= attackRadius) {
                    System.out.println("new plan");
                    return true;
                }
            }
        }

        System.out.println("no new plan needed");
        return false;
    }

}
