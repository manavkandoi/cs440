package src.pas.tetris.agents;


// SYSTEM IMPORTS
import java.util.Iterator;
import java.util.List;
import java.util.Random;


// JAVA PROJECT IMPORTS
import edu.bu.tetris.agents.QAgent;
import edu.bu.tetris.agents.TrainerAgent.GameCounter;
import edu.bu.tetris.game.Board;
import edu.bu.tetris.game.Game.GameView;
import edu.bu.tetris.game.minos.Mino;
import edu.bu.tetris.linalg.Matrix;
import edu.bu.tetris.linalg.Shape;
import edu.bu.tetris.nn.Model;
import edu.bu.tetris.nn.LossFunction;
import edu.bu.tetris.nn.Optimizer;
import edu.bu.tetris.nn.models.Sequential;
import edu.bu.tetris.nn.layers.Dense; // fully connected layer
import edu.bu.tetris.nn.layers.ReLU;  // some activations (below too)
import edu.bu.tetris.nn.layers.Tanh;
import edu.bu.tetris.nn.layers.Sigmoid;
import edu.bu.tetris.training.data.Dataset;
import edu.bu.tetris.utils.Pair;


public class TetrisQAgent
    extends QAgent
{

    public static final double EXPLORATION_PROB = 0.05;

    private Random random;

    public TetrisQAgent(String name)
    {
        super(name);
        this.random = new Random(12345); // optional to have a seed
    }

    public Random getRandom() { return this.random; }

    @Override
    public Model initQFunction() {
        // Enhanced model with additional features
        final int numFeatures = Board.NUM_ROWS * Board.NUM_COLS + 4; // 4 additional features
        final int hiddenDim = 4 * numFeatures; // More complex model
        final int outDim = 1;

        Sequential qFunction = new Sequential();
        qFunction.add(new Dense(numFeatures, hiddenDim));
        qFunction.add(new ReLU()); // Using ReLU for better performance in deeper networks
        qFunction.add(new Dense(hiddenDim, hiddenDim)); // Adding one more hidden layer
        qFunction.add(new Tanh());
        qFunction.add(new Dense(hiddenDim, outDim));
        return qFunction;
    }


    /**
        This function is for you to figure out what your features
        are. This should end up being a single row-vector, and the
        dimensions should be what your qfunction is expecting.
        One thing we can do is get the grayscale image
        where squares in the image are 0.0 if unoccupied, 0.5 if
        there is a "background" square (i.e. that square is occupied
        but it is not the current piece being placed), and 1.0 for
        any squares that the current piece is being considered for.
        
        We can then flatten this image to get a row-vector, but we
        can do more than this! Try to be creative: how can you measure the
        "state" of the game without relying on the pixels? If you were given
        a tetris game midway through play, what properties would you look for?
     */
    @Override
    public Matrix getQFunctionInput(final GameView game, final Mino potentialAction) {
        Matrix grayscaleImage = null;
        try {
            grayscaleImage = game.getGrayscaleImage(potentialAction).flatten();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

        // Use getShape() to retrieve the number of columns in the grayscale image
        Shape shape = grayscaleImage.getShape();
        int cols = shape.getNumCols(); // Assuming Shape has a method numCols() to get the number of columns

        // Create a new feature matrix with additional space for the 4 extra features
        Matrix featureMatrix = Matrix.zeros(1, cols + 4);

        // Copy grayscale image data to the feature matrix
        for (int i = 0; i < cols; i++) {
            featureMatrix.set(0, i, grayscaleImage.get(0, i));
        }

        // Calculate and add additional features
        featureMatrix.set(0, cols, calculateColumnHeights(game.getBoard()));
        featureMatrix.set(0, cols + 1, calculateNumberOfHoles(game.getBoard()));
        featureMatrix.set(0, cols + 2, calculateLinesClearedPotential(game.getBoard()));
        featureMatrix.set(0, cols + 3, calculateBumpiness(game.getBoard()));

        return featureMatrix;
    }

    private double calculateColumnHeights(Board board) {
        // Calculate the maximum height of each column
        double totalHeight = 0;
        for (int x = 0; x < Board.NUM_COLS; x++) {
            int height = 0;
            for (int y = 0; y < Board.NUM_ROWS; y++) {
                if (board.isCoordinateOccupied(x, y)) {
                    height = Board.NUM_ROWS - y;
                    break;
                }
            }
            totalHeight += height;
        }
        return totalHeight / Board.NUM_COLS;  // Average height
    }

    private double calculateNumberOfHoles(Board board) {
        // Calculate the number of "holes" in the board
        int holes = 0;
        for (int x = 0; x < Board.NUM_COLS; x++) {
            boolean blockFound = false;
            for (int y = 0; y < Board.NUM_ROWS; y++) {
                if (board.isCoordinateOccupied(x, y)) {
                    blockFound = true;
                } else if (blockFound) {
                    // This is a hole since there's space under a block
                    holes++;
                }
            }
        }
        return holes;
    }

    private double calculateLinesClearedPotential(Board board) {
        // Calculate the potential lines that could be cleared
        return board.clearFullLines().size();  // Using the clearFullLines just for potential count
    }

    private double calculateBumpiness(Board board) {
        // Calculate bumpiness between columns
        int bumpiness = 0;
        int previousHeight = -1;
        for (int x = 0; x < Board.NUM_COLS; x++) {
            int currentHeight = 0;
            for (int y = 0; y < Board.NUM_ROWS && board.isCoordinateOccupied(x, y); y++) {
                currentHeight++;
            }
            if (previousHeight != -1) {
                bumpiness += Math.abs(currentHeight - previousHeight);
            }
            previousHeight = currentHeight;
        }
        return bumpiness;
    }


    /**
     * This method is used to decide if we should follow our current policy
     * (i.e. our q-function), or if we should ignore it and take a random action
     * (i.e. explore).
     *
     * Remember, as the q-function learns, it will start to predict the same "good" actions
     * over and over again. This can prevent us from discovering new, potentially even
     * better states, which we want to do! So, sometimes we should ignore our policy
     * and explore to gain novel experiences.
     *
     * The current implementation chooses to ignore the current policy around 5% of the time.
     * While this strategy is easy to implement, it often doesn't perform well and is
     * really sensitive to the EXPLORATION_PROB. I would recommend devising your own
     * strategy here.
     */
   
    private double explorationRate = 0.05; // Start with some exploration rate

    @Override
    public boolean shouldExplore(final GameView game, final GameCounter gameCounter) {
        // Reduce exploration rate over time
        explorationRate *= 0.995; // Decay factor
        return this.getRandom().nextDouble() <= explorationRate;
    }



    /**
     * This method is a counterpart to the "shouldExplore" method. Whenever we decide
     * that we should ignore our policy, we now have to actually choose an action.
     *
     * You should come up with a way of choosing an action so that the model gets
     * to experience something new. The current implemention just chooses a random
     * option, which in practice doesn't work as well as a more guided strategy.
     * I would recommend devising your own strategy here.
     */
    @Override
    public Mino getExplorationMove(final GameView game) {
        List<Mino> options = game.getFinalMinoPositions();
        return options.get(this.getRandom().nextInt(options.size())); // Random move
    }

    /**
     * This method is called by the TrainerAgent after we have played enough training games.
     * In between the training section and the evaluation section of a phase, we need to use
     * the exprience we've collected (from the training games) to improve the q-function.
     *
     * You don't really need to change this method unless you want to. All that happens
     * is that we will use the experiences currently stored in the replay buffer to update
     * our model. Updates (i.e. gradient descent updates) will be applied per minibatch
     * (i.e. a subset of the entire dataset) rather than in a vanilla gradient descent manner
     * (i.e. all at once)...this often works better and is an active area of research.
     *
     * Each pass through the data is called an epoch, and we will perform "numUpdates" amount
     * of epochs in between the training and eval sections of each phase.
     */
    @Override
    public void trainQFunction(Dataset dataset,
                               LossFunction lossFunction,
                               Optimizer optimizer,
                               long numUpdates)
    {
        for(int epochIdx = 0; epochIdx < numUpdates; ++epochIdx)
        {
            dataset.shuffle();
            Iterator<Pair<Matrix, Matrix> > batchIterator = dataset.iterator();

            while(batchIterator.hasNext())
            {
                Pair<Matrix, Matrix> batch = batchIterator.next();

                try
                {
                    Matrix YHat = this.getQFunction().forward(batch.getFirst());

                    optimizer.reset();
                    this.getQFunction().backwards(batch.getFirst(),
                                                  lossFunction.backwards(YHat, batch.getSecond()));
                    optimizer.step();
                } catch(Exception e)
                {
                    e.printStackTrace();
                    System.exit(-1);
                }
            }
        }
    }

    /**
     * This method is where you will devise your own reward signal. Remember, the larger
     * the number, the more "pleasurable" it is to the model, and the smaller the number,
     * the more "painful" to the model.
     *
     * This is where you get to tell the model how "good" or "bad" the game is.
     * Since you earn points in this game, the reward should probably be influenced by the
     * points, however this is not all. In fact, just using the points earned this turn
     * is a **terrible** reward function, because earning points is hard!!
     *
     * I would recommend you to consider other ways of measuring "good"ness and "bad"ness
     * of the game. For instance, the higher the stack of minos gets....generally the worse
     * (unless you have a long hole waiting for an I-block). When you design a reward
     * signal that is less sparse, you should see your model optimize this reward over time.
     */
    @Override
    public double getReward(final GameView game) {
        double scoreReward = game.getScoreThisTurn();

        // Penalties for undesirable board states
        double heightPenalty = calculateHeightPenalty(game.getBoard());
        double holesPenalty = calculateNumberOfHoles(game.getBoard()) * 10; // More weight on avoiding holes
        double bumpinessPenalty = calculateBumpiness(game.getBoard()) * 2; // Moderate weight on bumpiness

        // Combine all components to compute the final reward
        double totalReward = scoreReward - heightPenalty - holesPenalty - bumpinessPenalty;
        return totalReward;
    }

    private double calculateHeightPenalty(Board board) {
        double penalty = 0;
        int maxHeight = 0;
        for (int x = 0; x < Board.NUM_COLS; x++) {
            int height = 0;
            for (int y = 0; y < Board.NUM_ROWS; y++) {
                if (board.isCoordinateOccupied(x, y)) {
                    height = Board.NUM_ROWS - y;
                    break;
                }
            }
            maxHeight = Math.max(maxHeight, height);
        }
        penalty = maxHeight > 15 ? (maxHeight - 15) * 10 : 0; // Penalizing heights above 15 rows
        return penalty;
    }

}
