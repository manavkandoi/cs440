package src.labs.stealth.agents;

// SYSTEM IMPORTS
import edu.bu.labs.stealth.agents.MazeAgent;
import edu.bu.labs.stealth.graph.Vertex;
import edu.bu.labs.stealth.graph.Path;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State.StateView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;       // will need for bfs
import java.util.Queue;         // will need for bfs
import java.util.LinkedList;    // will need for bfs
import java.util.List;
import java.util.Map;
import java.util.Set;           // will need for bfs

// JAVA PROJECT IMPORTS


public class BFSMazeAgent
    extends MazeAgent
{

    public BFSMazeAgent(int playerNum)
    {
        super(playerNum);
    }

    @Override
    public Path search(Vertex src,
                       Vertex goal,
                       StateView state)
    {


    Queue<Path> queue = new LinkedList<>();
    Map<Vertex, Path> visited = new HashMap<>(); //set of ndes already visited

    //initialize path, containnig only src node at the moment
    Path initPath = new Path(src,0.0F, null);
    queue.add(initPath);
    visited.put(src, initPath);
    

    //keep removing the elemeent of queue as long as its not empty
    while (!queue.isEmpty()) {
        Path currentPath = queue.poll(); 
        Vertex currentVertex = currentPath.getDestination();

        // If we reach a vertex adjecent to the goal, return the current path
        if (isAdjacent(currentVertex, goal)) {
            return currentPath; 
        }

        //itereate though neighbors of the vertex, to move the the next neighborhood
        for (Vertex neighbor : getNeighbors(currentVertex, state)) {
            if (!visited.containsKey(neighbor)) {
                float edgeWeight = 1.0F; // Edge weight for BFS is given 1
                Path newPath = new Path(neighbor, edgeWeight, currentPath); //add to the path
                visited.put(neighbor, newPath);
                queue.add(newPath);
            }
        }
    }
    // If no path is found, return null
    return null;
    }


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
