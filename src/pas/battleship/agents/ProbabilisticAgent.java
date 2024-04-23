package src.pas.battleship.agents;

import edu.bu.battleship.agents.Agent;
import edu.bu.battleship.game.Game.GameView;
import edu.bu.battleship.utils.Coordinate;

import edu.bu.battleship.game.EnemyBoard.Outcome;

public class ProbabilisticAgent extends Agent {
    private double[][] probabilityGrid;
    private boolean[][] guessedCells;
    private int boardSize = 10; // Assuming a 10x10 board for simplicity.

    public ProbabilisticAgent(String name) {
        super(name);
        guessedCells = new boolean[boardSize][boardSize];
        probabilityGrid = new double[boardSize][boardSize];
        initializeProbabilityGrid();
    }

    private void initializeProbabilityGrid() {
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                if ((row + col) % 2 == 0) { // Checkerboard pattern
                    probabilityGrid[row][col] = 1.0;
                } else {
                    probabilityGrid[row][col] = 0.0;
                }
            }
        }
    }

    @Override
    public Coordinate makeMove(final GameView game) {
        Outcome[][] enemyOutcomes = game.getEnemyBoardView();

        // Update probabilities based on the outcome of previous moves
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                if (enemyOutcomes[row][col] == Outcome.HIT) {
                    increaseProbabilityAroundHit(row, col);
                } else if (enemyOutcomes[row][col] == Outcome.MISS) {
                    probabilityGrid[row][col] = 0; // Misses should not be chosen again
                }
                // Assume UNKNOWN or null means unguessed
            }
        }

        // Find the highest probability cell that hasn't been guessed
        double maxProbability = -1;
        Coordinate nextMove = null;
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                if (!guessedCells[row][col] && probabilityGrid[row][col] > maxProbability) {
                    maxProbability = probabilityGrid[row][col];
                    nextMove = new Coordinate(row, col);
                }
            }
        }

        if (nextMove == null) {
            throw new IllegalStateException("No valid moves available");
        }

        // Mark the chosen cell as guessed
        guessedCells[nextMove.getXCoordinate()][nextMove.getYCoordinate()] = true;
        return nextMove;
    }

    private void increaseProbabilityAroundHit(int row, int col) {
        int[] dRow = {-1, 1, 0, 0};
        int[] dCol = {0, 0, -1, 1};
        for (int i = 0; i < dRow.length; i++) {
            int newRow = row + dRow[i];
            int newCol = col + dCol[i];
            if (newRow >= 0 && newRow < boardSize && newCol >= 0 && newCol < boardSize && !guessedCells[newRow][newCol]) {
                probabilityGrid[newRow][newCol] += 2; // Increase probability for cells around a hit
            }
        }
    }

    @Override
    public void afterGameEnds(final GameView game) {
        // Reset for the next game
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                guessedCells[row][col] = false;
                probabilityGrid[row][col] = (row + col) % 2 == 0 ? 1.0 : 0.0;
            }
        }
    }
}
