package model;

import control.EstimatorInterface;

import java.util.Random;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class HMMLocalizer implements EstimatorInterface {

	private int rows, cols, heads;
	private int heading;
	private int[] position = new int[2];
	private Random numGenerator;

	private static final int[][] HEADINGS = new int[][] { { -1,0 }, { 0,1 }, { 1,0 }, { 0,-1 } };
	
	private static final int[][] PRIMARY_RING = new int[][] { {-1,-1}, {-1, 0}, {-1, 1}, {0, 1}, {1, 1}, {1, 0}, {1, -1}, {0, -1} };
	private static final int[][] SECONDARY_RING = new int[][] { {-2,-2}, {-2, -1}, {-2, 0}, {-2, 1}, {-2, 2}, {-1, 2}, {0, 2}, {1, 2}, {2, 2}, {2, 1}, {2, 0}, {2, -1}, {2, -2}, {1, -2}, {0, -2}, {-1, -2} };
	
	private HashMap<int[], double[][]> observationMatrices;
	
	public HMMLocalizer(int rows, int cols, int heads) {
		this.rows = rows;
		this.cols = cols;
		this.heads = heads;

		position[0] = 0;
		position[1] = 0;
		
		numGenerator = new Random();
		heading = numGenerator.nextInt(4);
		
		initializeObservationMatrices();
	}
	
	private void initializeObservationMatrices() {
		observationMatrices = new HashMap<int[], double[][]>();
		
		for (int matrixRowIndex = 0; matrixRowIndex < rows; matrixRowIndex++) {
			for (int matrixColIndex = 0; matrixColIndex < cols; matrixColIndex++) {
				double[][] newMatrix = new double[rows][cols];

				for (int row = 0; row < rows; row++) {
					for (int col = 0; col < cols; col++) {
						double distanceSquared = Math.pow(row - matrixRowIndex, 2) + Math.pow(col - matrixColIndex,  2);
						int distance =  (int) Math.sqrt(distanceSquared);
						
						if (distance == 0) { newMatrix[row][col] = 0.1; }
						else if (distance == 1) { newMatrix[row][col] = 0.05; }
						else if (distance == 2) { newMatrix[row][col] = 0.025; }
						else { newMatrix[row][col] = 0.0; }
					}
				}
				
				observationMatrices.put(new int[] {matrixRowIndex,  matrixColIndex}, newMatrix);
			}
			
		}
		
		for (int[] key : observationMatrices.keySet()) {
			System.out.println(String.format("Matrix for (%d, %d)", key[0], key[1])); 
			double[][] matrix = observationMatrices.get(key);
			for (int i = 0; i< matrix.length; i++) {
			    for (int j = 0; j < matrix[i].length; j++) {
			        System.out.print(matrix[i][j] + "  ");
			    }
			        System.out.println();
			}
		}
		
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

		System.out.println("Updating");
		updateHeadingAndPosition();

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
		if (row == rows)
			possibleHeadings.remove(new Integer(2)); // remove south
		if (col == 0)
			possibleHeadings.remove(new Integer(3)); // remove west
		if (col == cols)
			possibleHeadings.remove(new Integer(1)); // remove east
		
//		System.out.println(String.format("Possible Headings for %d, %d: %s", row, col, possibleHeadings.toString()));
		
		return possibleHeadings;
	}

	@Override
	public int[] getCurrentTrueState() {
		return new int[] {position[0], position[1], heading};
	}

	@Override
	public int[] getCurrentReading() {
		numGenerator = new Random();
		
		ArrayList<int[]> possibleReadings = new ArrayList<int[]>();
		
		for (int i = 0; i < 4; i++) possibleReadings.add(position);
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < PRIMARY_RING.length; j++) possibleReadings.add(PRIMARY_RING[j]);
		}
		for (int i = 0; i < SECONDARY_RING.length; i++) possibleReadings.add(SECONDARY_RING[i]);
		
		// possibleReadings List should be of size 36 now
		
		int randNum = numGenerator.nextInt(40);
		
		if (randNum >= 36) return null;
		if (randNum < 4) return position;
		
		int[] ringCoordShift = possibleReadings.get(randNum);
		int[] newRingCoord = new int[] { position[0] + ringCoordShift[0], position[1] + ringCoordShift[1] };
		
		if (inBounds(newRingCoord[0], newRingCoord[1])) {
			return newRingCoord;
		}
		
		return null;		
	}

	@Override
	public double getCurrentProb(int x, int y) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getOrXY(int rX, int rY, int x, int y, int h) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getTProb(int x, int y, int h, int nX, int nY, int nH) {
		int dx = nX - x;
		int dy = nY - y;
		
		HashMap<Integer, Double> probMap = getProbMap(x, y, h);
		
		if (probMap.containsKey(nH) && HEADINGS[nH][0] == dx && HEADINGS[nH][1] == dy) {
				System.out.println("---T PROB ---");
				System.out.println(String.format("(x, y, h) -- (%d, %d, %d)", x, y, h));
				System.out.println(String.format("(nX, nY, nH) -- (%d, %d, %d)", nX, nY, nH));		
				System.out.println("Prob Map: " + probMap.toString());
				return probMap.get(nH);
		}
		
		return 0.0;
	}
	
	private boolean inBounds(int row, int col) {
		return row >= 0 && row < rows && col >= 0 && col < cols;
	}
	
	private boolean newLocationInBounds(int row, int col, int dRow, int dCol) {
		return inBounds (row + dRow, col + dCol);
	}

}
