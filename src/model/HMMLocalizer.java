package model;

import control.EstimatorInterface;

import java.util.Random;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HMMLocalizer implements EstimatorInterface {

	private int iteration, numCorrect;
	private int rows, cols, heads;
	private int heading;
	private int[] position = new int[2];
	private Random numGenerator;

	// North, East, South, West
	private static final int[][] HEADINGS = new int[][] { { -1, 0 }, { 0, 1 }, { 1, 0 }, { 0, -1 } };

	private static final int[][] PRIMARY_RING = new int[][] { { -1, -1 }, { -1, 0 }, { -1, 1 }, { 0, 1 }, { 1, 1 },
			{ 1, 0 }, { 1, -1 }, { 0, -1 } };
	private static final int[][] SECONDARY_RING = new int[][] { { -2, -2 }, { -2, -1 }, { -2, 0 }, { -2, 1 }, { -2, 2 },
			{ -1, 2 }, { 0, 2 }, { 1, 2 }, { 2, 2 }, { 2, 1 }, { 2, 0 }, { 2, -1 }, { 2, -2 }, { 1, -2 }, { 0, -2 },
			{ -1, -2 } };

	private HashMap<List<Integer>, double[][]> observationMatrices;

	private double[][] transitionMatrix;
	private double[] fVector;

	public HMMLocalizer(int rows, int cols, int heads) {
		this.iteration = 0;
		this.numCorrect = 0;
		
		this.rows = rows;
		this.cols = cols;
		this.heads = heads;

		position[0] = 0;
		position[1] = 0;

		numGenerator = new Random();
		heading = numGenerator.nextInt(4);

		initializeObservationMatrices();
		initializeFVector();
		initializeTransitionMatrix();
	}

	private void initializeObservationMatrices() {
		observationMatrices = new HashMap<List<Integer>, double[][]>();

		for (int matrixRowIndex = 0; matrixRowIndex < rows; matrixRowIndex++) {
			for (int matrixColIndex = 0; matrixColIndex < cols; matrixColIndex++) {
				double[][] newMatrix = new double[rows][cols];

				for (int row = 0; row < rows; row++) {
					for (int col = 0; col < cols; col++) {
						double distanceSquared = Math.pow(row - matrixRowIndex, 2) + Math.pow(col - matrixColIndex, 2);
						int distance = (int) Math.sqrt(distanceSquared);

						if (distance == 0) {
							newMatrix[row][col] = 0.1;
						} else if (distance == 1) {
							newMatrix[row][col] = 0.05;
						} else if (distance == 2) {
							newMatrix[row][col] = 0.025;
						} else {
							newMatrix[row][col] = 0.0;
						}
					}
				}

				List<Integer> readingCoord = new ArrayList<Integer>();
				readingCoord.add(matrixRowIndex);
				readingCoord.add(matrixColIndex);
				observationMatrices.put(readingCoord, newMatrix);
			}
		}

		List<Integer> nothingCoord = new ArrayList<Integer>();
		nothingCoord.add(-1);
		nothingCoord.add(-1);
		observationMatrices.put(nothingCoord, getNothingMatrix());

		for (List<Integer> key : observationMatrices.keySet()) {
			// System.out.println(String.format("Matrix for (%d, %d)", key.get(0),
			// key.get(1)));
			double[][] matrix = observationMatrices.get(key);
			for (int i = 0; i < matrix.length; i++) {
				for (int j = 0; j < matrix[i].length; j++) {
					// System.out.print(matrix[i][j] + " ");
				}
				// System.out.println();
			}
		}
	}

	private double[][] getNothingMatrix() {
		double[][] nothingMatrix = new double[rows][cols];

		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				double prob = 1.0 - 0.1 - 0.05 * getPrimaryRingSize(row, col) - 0.025 * getSecondaryRingSize(row, col);
				nothingMatrix[row][col] = prob;
			}
		}
		
		return nothingMatrix;
	}

	private double[][] getObservationMatrix(int row, int col) {
		List<Integer> readingCoord = new ArrayList<Integer>();
		readingCoord.add(row);
		readingCoord.add(col);
		double[][] matrix = observationMatrices.get(readingCoord);
		return matrix;
	}

	private double[][] getDiagonalObservationMatrix(int row, int col) {
		// System.out.println(String.format("Obs matrix for %d, %d", row, col));
		double[][] obsMatrix = getObservationMatrix(row, col);
		// printMatrix(obsMatrix);

		int len = rows * cols * heads;
		double[][] diagMatrix = new double[len][len];

		for (int i = 0; i < len; i++) {
			for (int j = 0; j < len; j++) {
				diagMatrix[i][j] = 0.0;
			}
		}

		for (int i = 0; i < len; i++) {
			int[] vals = getValsFromIndex(i);
			diagMatrix[i][i] = obsMatrix[vals[0]][vals[1]];
		}

		return diagMatrix;
	}

	private int getPrimaryRingSize(int row, int col) {
		int count = 0;
		for (int[] direction : PRIMARY_RING) {
			int newRow = row + direction[0];
			int newCol = col + direction[1];
			if (inBounds(newRow, newCol))
				count++;
		}

		return count;
	}

	private int getSecondaryRingSize(int row, int col) {
		int count = 0;
		for (int[] direction : SECONDARY_RING) {
			int newRow = row + direction[0];
			int newCol = col + direction[1];
			if (inBounds(newRow, newCol))
				count++;
		}

		return count;
	}

	private void initializeFVector() {
		fVector = new double[rows * cols * heads];
		double initialProb = 1.0 / (rows * cols * heads);
		for (int i = 0; i < fVector.length; i++) {
			fVector[i] = initialProb;
		}
	}

	private void initializeTransitionMatrix() {
		transitionMatrix = new double[rows * cols * heads][rows * cols * heads];

		for (int i = 0; i < transitionMatrix.length; i++) {
			int[] iVals = getValsFromIndex(i);
			// System.out.println("Index " + i + ": "+ Arrays.toString(iVals));
			for (int j = 0; j < transitionMatrix[i].length; j++) {
				int[] jVals = getValsFromIndex(j);
				transitionMatrix[i][j] = getTProb(iVals[0], iVals[1], iVals[2], jVals[0], jVals[1], jVals[2]);
			}
		}

		// System.out.println("Transition Matrix:");
		// printMatrix(transitionMatrix);

		transitionMatrix = transposeMatrix(transitionMatrix);
	}

	private void printMatrix(double[][] array) {
		for (int x = 0; x < array.length; x++) {
			for (int y = 0; y < array[x].length; y++) {
				System.out.print(array[x][y] + " ");
			}
			System.out.println();
		}
		System.out.println();
	}

	private void printVector(double[] array) {
		System.out.println();
		for (int i = 0; i < array.length; i++) {
			System.out.print(array[i] + " ");
		}
		System.out.println();
	}

	private int[] getValsFromIndex(int index) {
		int head = index % heads;

		index -= head;

		int col = (index % (rows * heads)) / heads;
		index -= (col * heads);

		int row = index / (rows * heads);

		return new int[] { row, col, head };
	}

	private int getIndexFromRowColHead(int row, int col, int head) {
		int start = row * (rows * heads);
		start += col * heads;
		start += head;
		return start;
	}

	@Override
	public int getNumRows() {
		return rows;
	}

	@Override
	public int getNumCols() {
		return cols;
	}

	@Override
	public int getNumHead() {
		return heads;
	}

	@Override
	public void update() {
		iteration++;
		
		updateHeadingAndPosition();
		updateFVector();
		
		double[] prediction = getPrediction();
		int predictionPercent = (int) (prediction[2] * 100.0);
		int[] predictionCoords = new int[] {(int) prediction[0], (int) prediction[1]};
		
		if (predictionCoords[0] == position[0] && predictionCoords[1] == position[1]) {
			numCorrect++;
		}
		
		int percentCorrect = (int) (((double) (numCorrect) / (double) iteration) * 100);
		
		System.out.println(String.format("Iteration: %d", iteration));
		System.out.println(String.format("Position: (%d, %d)", position[0], position[1]));
		System.out.println(String.format("Prediction: (%d, %d) @ %d%%", predictionCoords[0], predictionCoords[1], predictionPercent));
		System.out.println(String.format("Euclidean Distance: %f", getEuclideanDistance(position, predictionCoords)));
		System.out.println(String.format("Manhattan Distance: %d", getManhattanDistance(position, predictionCoords)));
		System.out.println(String.format("Num Correct: %d", numCorrect));
		System.out.println(String.format("Percent Correct Predictions: %d%%", percentCorrect));
		
		System.out.println();
	}

	private void updateHeadingAndPosition() {
		HashMap<Integer, Double> probMap = getProbMap(position[0], position[1], heading);
		ArrayList<Integer> headingsToChoose = new ArrayList<Integer>();

		for (Integer headingVal : probMap.keySet()) {
			int headingWeight = (int) (probMap.get(headingVal) * 100);
			for (int i = 0; i < headingWeight; i++) {
				headingsToChoose.add(headingVal);
			}
		}

		numGenerator = new Random();
		int nextHeadingIndex = numGenerator.nextInt(headingsToChoose.size());

		int nextHeading = headingsToChoose.get(nextHeadingIndex);
		heading = nextHeading;

		position[0] = position[0] + HEADINGS[heading][0];
		position[1] = position[1] + HEADINGS[heading][1];
	}

	private void updateFVector() {
		int[] currentReading = getCurrentReading();
		if (currentReading == null)
			currentReading = new int[] { -1, -1 };
		// printMatrix(observationMatrix);

		double[][] diagObservationMatrix = getDiagonalObservationMatrix(currentReading[0], currentReading[1]);
		// System.out.println("DIAG");
		// printMatrix(diagObservationMatrix);

		double[][] transposedTransitionMatrix = transitionMatrix;

		double[][] multipliedTranspose = multiplyMatrix(diagObservationMatrix, transposedTransitionMatrix);

		double[] newFVector = multiplyMatrix(multipliedTranspose, fVector);

		double sum = 0.0;
		for (int i = 0; i < newFVector.length; i++) {
			sum += newFVector[i];
		}
		for (int i = 0; i < newFVector.length; i++) {
			newFVector[i] = newFVector[i] / sum;
		}

		fVector = newFVector;

		// System.out.println("NewFVector");
		// printVector(fVector);
	}

	private HashMap<Integer, Double> getProbMap(int row, int col, int head) {
		HashMap<Integer, Double> probMap = new HashMap<Integer, Double>();

		for (Integer headingDir : getPossibleHeadings(row, col)) {
			probMap.put(headingDir, 0.0);
		}

		if (probMap.keySet().contains(head)) {
			probMap.put(head, 0.7);
			int numOtherHeadings = probMap.keySet().size() - 1;
			for (Integer headingVal : probMap.keySet()) {
				if (headingVal != head)
					probMap.put(headingVal, 0.3 / numOtherHeadings);
			}
		}

		else {
			int numHeadings = probMap.keySet().size();
			for (Integer headingVal : probMap.keySet()) {
				probMap.put(headingVal, 1.0 / numHeadings);
			}
		}

		return probMap;
	}

	private ArrayList<Integer> getPossibleHeadings(int row, int col) {
		ArrayList<Integer> possibleHeadings = new ArrayList<Integer>();
		possibleHeadings.addAll(Arrays.asList(0, 1, 2, 3));

		if (row == 0)
			possibleHeadings.remove(new Integer(0)); // remove north
		if (row == rows - 1)
			possibleHeadings.remove(new Integer(2)); // remove south
		if (col == 0)
			possibleHeadings.remove(new Integer(3)); // remove west
		if (col == cols - 1)
			possibleHeadings.remove(new Integer(1)); // remove east

		// System.out.println(String.format("Possible Headings for %d, %d: %s", row,
		// col, possibleHeadings.toString()));

		return possibleHeadings;
	}

	@Override
	public int[] getCurrentTrueState() {
		return new int[] { position[0], position[1], heading };
	}

	@Override
	public int[] getCurrentReading() {
		numGenerator = new Random();

		ArrayList<int[]> possibleReadings = new ArrayList<int[]>();

		for (int i = 0; i < 4; i++)
			possibleReadings.add(position);
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < PRIMARY_RING.length; j++)
				possibleReadings.add(PRIMARY_RING[j]);
		}
		for (int i = 0; i < SECONDARY_RING.length; i++)
			possibleReadings.add(SECONDARY_RING[i]);

		// possibleReadings List should be of size 36 now

		int randNum = numGenerator.nextInt(40);

		if (randNum >= 36)
			return null;
		if (randNum < 4)
			return position;

		int[] ringCoordShift = possibleReadings.get(randNum);
		int[] newRingCoord = new int[] { position[0] + ringCoordShift[0], position[1] + ringCoordShift[1] };

		if (inBounds(newRingCoord[0], newRingCoord[1])) {
			return newRingCoord;
		}

		return null;
	}

	@Override
	public double getCurrentProb(int x, int y) {
		double summedProb = 0.0;
		int index = getIndexFromRowColHead(x, y, 0);

		for (int i = 0; i < 4; i++) {
			summedProb += fVector[index + i];
		}

		return summedProb;
	}
	
	private double[] getPrediction() {
		double[][] summedProbs = getSummedProbs();
		double maxProb = 0.0;
		int maxRow = 0, maxCol = 0;
		
//		printMatrix(summedProbs);
		
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				if (summedProbs[row][col] > maxProb) {
					maxProb = summedProbs[row][col];
					maxRow = row;
					maxCol = col;
				}
			}
		}
		
		return new double[] {maxRow, maxCol, maxProb};
	}

	private double[][] getSummedProbs() {
		double[][] summedProbs = new double[rows][cols];
			
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				double summedProb = 0.0;
				int index = getIndexFromRowColHead(row, col, 0);
				summedProbs[row][col] = getCurrentProb(row, col);
			}
		}
		
		return summedProbs;
	}
	
	@Override
	public double getOrXY(int rX, int rY, int x, int y, int h) {		
		if (rX == -1 && rY == -1) {
			return getObservationMatrix(-1, -1)[x][y];
		}
		
		double distanceSquared = Math.pow(x - rX, 2) + Math.pow(y - rY, 2);
		int distance = (int) Math.sqrt(distanceSquared);

		if (distance == 0)
			return 0.1;
		if (distance == 1)
			return 0.05;
		if (distance == 2)
			return 0.025;

		return 0.0;
	}
	
	private double getEuclideanDistance(int[] a, int[] b) {
		double distanceSquared = Math.pow(a[0] - b[0], 2) + Math.pow(a[1] - b[1], 2);
		double distance = Math.sqrt(distanceSquared);
		return distance;		
	}
	
	private int getManhattanDistance(int[] a, int[] b) {
		return (Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]));
	}

	@Override
	public double getTProb(int x, int y, int h, int nX, int nY, int nH) {
		int dx = nX - x;
		int dy = nY - y;

		HashMap<Integer, Double> probMap = getProbMap(x, y, h);

		if (probMap.containsKey(nH) && HEADINGS[nH][0] == dx && HEADINGS[nH][1] == dy) {
			// System.out.println("---T PROB ---");
			// System.out.println(String.format("(x, y, h) -- (%d, %d, %d)", x, y, h));
			// System.out.println(String.format("(nX, nY, nH) -- (%d, %d, %d)", nX, nY,
			// nH));
			// System.out.println("Prob Map: " + probMap.toString());
			return probMap.get(nH);
		}

		return 0.0;
	}

	private boolean inBounds(int row, int col) {
		return row >= 0 && row < rows && col >= 0 && col < cols;
	}

	// The following 3 matrix methods included under fair use guidelines
	// to prevent needing to use a matrix library
	// https://introcs.cs.princeton.edu/java/22library/Matrix.java.html

	// From https://introcs.cs.princeton.edu/java/22library/Matrix.java.html
	public static double[][] transposeMatrix(double[][] a) {
		int m = a.length;
		int n = a[0].length;
		double[][] b = new double[n][m];
		for (int i = 0; i < m; i++)
			for (int j = 0; j < n; j++)
				b[j][i] = a[i][j];
		return b;
	}

	// From https://introcs.cs.princeton.edu/java/22library/Matrix.java.html
	public static double[][] multiplyMatrix(double[][] a, double[][] b) {
		int m1 = a.length;
		int n1 = a[0].length;
		int m2 = b.length;
		int n2 = b[0].length;
		if (n1 != m2)
			throw new RuntimeException("Illegal matrix dimensions.");
		double[][] c = new double[m1][n2];
		for (int i = 0; i < m1; i++)
			for (int j = 0; j < n2; j++)
				for (int k = 0; k < n1; k++)
					c[i][j] += a[i][k] * b[k][j];
		return c;
	}

	// From https://introcs.cs.princeton.edu/java/22library/Matrix.java.html
	public static double[] multiplyMatrix(double[][] a, double[] x) {
		int m = a.length;
		int n = a[0].length;
		if (x.length != n)
			throw new RuntimeException("Illegal matrix dimensions.");
		double[] y = new double[m];
		for (int i = 0; i < m; i++)
			for (int j = 0; j < n; j++)
				y[i] += a[i][j] * x[j];
		return y;
	}

}
