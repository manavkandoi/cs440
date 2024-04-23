package src.labs.stealth.agents;

// SYSTEM IMPORTS
import edu.bu.labs.stealth.agents.MazeAgent;
import edu.bu.labs.stealth.graph.Vertex;
import edu.bu.labs.stealth.graph.Path;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State.StateView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;   // will need for dfs
import java.util.List;
import java.util.Map;
import java.util.Stack;     // will need for dfs
import java.util.Set;       // will need for dfs

// JAVA PROJECT IMPORTS


public class DFSMazeAgent
    extends MazeAgent
{

    public DFSMazeAgent(int playerNum)
    {
        super(playerNum);
    }

    @Override
    public Path search(Vertex src,
                       Vertex goal,
                       StateView state){

                       
    Stack<Path> stack = new Stack<>();
    Map<Vertex, Path> visited = new HashMap<>();
    Path initPath = new Path(src, 0.0F, null); //initialize path, with the source node in it
    stack.push(initPath);
    visited.put(src, initPath);
    while (!stack.isEmpty()) {
        Path currentPath = stack.pop(); // Use stack to get the current path

        Vertex currentVertex = currentPath.getDestination();

        // Check if the current vertex is adjacent to the goal, if so return the current path
        if (isAdjacent(currentVertex, goal)) {
            return currentPath; 
        }



        for (Vertex neighbor : getNeighbors(currentVertex, state)) {
            if (!visited.containsKey(neighbor)) {
                float edgeWeight = 1.0F; // Edge weight for DFS is given 1

                Path newPath = new Path(neighbor, edgeWeight, currentPath);
                visited.put(neighbor, newPath); //note of the new vertex visited
                stack.push(newPath); // Push path onto the stack

            }

        }
    }

    // If no path is found, return null or handle it as needed.
    return null;
}

//helper functiosn same as BFSmazeagent
    //function to get neighbors in all 8 directions of a vertex(N, E, NW, SE, etc)
    private List<Vertex> getNeighbors(Vertex vertex, StateView state) {
        List<Vertex> neighbors = new ArrayList<>();
        // Define the possible directions a unit can move (8 directions)
        int[][] directions = {{0, 1}, {0, -1}, {-1, 0}, {1, 0}, {-1, 1}, {1, 1}, {-1, -1}, {1, -1}};
        
        // Get a list of all tree nodes from the state view
        List<ResourceNode.ResourceView> trees = state.getResourceNodes(ResourceNode.Type.TREE);
    
        for (int[] direction : directions) {
            int newX = vertex.getXCoordinate() + direction[0];
            int newY = vertex.getYCoordinate() + direction[1];
    
            // Create a new potential neighbor vertex
            Vertex potentialNeighbor = new Vertex(newX, newY);
    
            // Check if the potential neighbor is a valid position and not a tree
            if (isValidPosition(potentialNeighbor, state, trees)) {
                neighbors.add(potentialNeighbor);
            }
        }
    
        return neighbors;
    }

    //checks if two vertices are adjecent to one another
    private boolean isAdjacent(Vertex current, Vertex goal) {
        // Check if current is adjacent to goal (one unit away in any direction)
        return Math.abs(current.getXCoordinate() - goal.getXCoordinate()) <= 1 &&
               Math.abs(current.getYCoordinate() - goal.getYCoordinate()) <= 1 &&
               !(current.equals(goal)); 
    }
    //function checks if a potential neighbor is a valid neighbor
    private boolean isValidPosition(Vertex vertex, StateView state, List<ResourceNode.ResourceView> trees) {
    // Check if the position is within the map bounds
    if (vertex.getXCoordinate() < 0 || vertex.getXCoordinate() >= state.getXExtent() ||
        vertex.getYCoordinate() < 0 || vertex.getYCoordinate() >= state.getYExtent()) {
        return false;
    }
    // Check if the coordinates are not the same as a tree node
    for (ResourceNode.ResourceView tree : trees) {
        if (tree.getXPosition() == vertex.getXCoordinate() && tree.getYPosition() == vertex.getYCoordinate()) {
            return false; //if the position is a tree, then its not a valid neighbor
        }
    }
    return true; // The position is within bounds and not a tree, so it's valid
}


    @Override
    public boolean shouldReplacePlan(StateView state)
    {
        return false;
    }

}
