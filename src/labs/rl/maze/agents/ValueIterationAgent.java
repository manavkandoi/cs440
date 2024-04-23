package src.labs.rl.maze.agents;


// SYSTEM IMPORTS
import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.util.Direction;


import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


// JAVA PROJECT IMPORTS
import edu.bu.labs.rl.maze.agents.StochasticAgent;
import edu.bu.labs.rl.maze.agents.StochasticAgent.RewardFunction;
import edu.bu.labs.rl.maze.agents.StochasticAgent.TransitionModel;
import edu.bu.labs.rl.maze.utilities.Coordinate;
import edu.bu.labs.rl.maze.utilities.Pair;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ValueIterationAgent
    extends StochasticAgent
{

    public static final double GAMMA = 1; // feel free to change this around!
    public static final double EPSILON = 1e-6; // don't change this though

    private Map<Coordinate, Double> utilities;

	public ValueIterationAgent(int playerNum)
	{
		super(playerNum);
        this.utilities = null;
	}

    public Map<Coordinate, Double> getUtilities() { return this.utilities; }
    private void setUtilities(Map<Coordinate, Double> u) { this.utilities = u; }

    public boolean isTerminalState(Coordinate c)
    {
        return c.equals(StochasticAgent.POSITIVE_TERMINAL_STATE)
            || c.equals(StochasticAgent.NEGATIVE_TERMINAL_STATE);
    }

    /**
     * A method to get an initial utility map where every coordinate is mapped to the utility 0.0
     */
    public Map<Coordinate, Double> getZeroMap(StateView state)
    {
        Map<Coordinate, Double> m = new HashMap<Coordinate, Double>();
        for(int x = 0; x < state.getXExtent(); ++x)
        {
            for(int y = 0; y < state.getYExtent(); ++y)
            {
                if(!state.isResourceAt(x, y))
                {
                    // we can go here
                    m.put(new Coordinate(x, y), 0.0);
                }
            }
        }
        return m;
    }

    public void valueIteration(StateView state) {
        Map<Coordinate, Double> newUtilities = getZeroMap(state);
        newUtilities.put(StochasticAgent.POSITIVE_TERMINAL_STATE, 1.0);
        newUtilities.put(StochasticAgent.NEGATIVE_TERMINAL_STATE, -1.0);

        boolean continueIteration;
        double maxChange;

        do {
            continueIteration = false;
            maxChange = 0.0;
            Map<Coordinate, Double> utilitiesCopy = new HashMap<>(newUtilities); // Use a copy for reference during updates

            for (Coordinate c : newUtilities.keySet()) {
                if (isTerminalState(c)) continue;

                double maxUtility = Double.NEGATIVE_INFINITY;
                for (Direction d : TransitionModel.CARDINAL_DIRECTIONS) {
                    double expectedUtility = 0.0;
                    for (Pair<Coordinate, Double> transition : TransitionModel.getTransitionProbs(state, c, d)) {
                        double utilityNextState = utilitiesCopy.getOrDefault(transition.getFirst(), 0.0);
                        expectedUtility += transition.getSecond() * utilityNextState;
                    }
                    maxUtility = Math.max(maxUtility, expectedUtility);
                }

                double updatedUtility = RewardFunction.getReward(c) + GAMMA * maxUtility;
                updatedUtility = roundToSixDecimalPlaces(updatedUtility); // Round to six decimal places

                double change = Math.abs(utilitiesCopy.get(c) - updatedUtility);
                newUtilities.put(c, updatedUtility);

                if (change > maxChange) {
                    maxChange = change;
                }
            }

            if (maxChange > EPSILON) {
                continueIteration = true;
            }
        } while (continueIteration);

        setUtilities(newUtilities);
    }

    private double roundToSixDecimalPlaces(double value) {
        return Math.round(value * 1000000) / 1000000;
    }







    @Override
    public void computePolicy(StateView state,
                              HistoryView history)
    {
        // compute the utilities
        this.valueIteration(state);

        // compute the policy from the utilities
        Map<Coordinate, Direction> policy = new HashMap<Coordinate, Direction>();

        for(Coordinate c : this.getUtilities().keySet())
        {
            // figure out what to do when in this state
            double maxActionUtility = Double.NEGATIVE_INFINITY;
            Direction bestDirection = null;

            // go over every action
            for(Direction d : TransitionModel.CARDINAL_DIRECTIONS)
            {

                // measure how good this action is as a weighted combination of future state's utilities
                double thisActionUtility = 0.0;
                for(Pair<Coordinate, Double> transition : TransitionModel.getTransitionProbs(state, c, d))
                {
                    thisActionUtility += transition.getSecond() * this.getUtilities().get(transition.getFirst());
                }

                // keep the best one!
                if(thisActionUtility > maxActionUtility)
                {
                    maxActionUtility = thisActionUtility;
                    bestDirection = d;
                }
            }

            // policy recommends the best action for every state
            policy.put(c, bestDirection);
        }

        this.setPolicy(policy);
    }

}
