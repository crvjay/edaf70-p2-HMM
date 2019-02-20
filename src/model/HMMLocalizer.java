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

	private static final int[][] HEADINGS = new int[][] { { -1, 0 }, { 0, 1 }, { 1, 0 }, { 0, -1 } };

	public HMMLocalizer(int rows, int cols, int heads) {
		this.rows = rows;
		this.cols = cols;
		this.heads = heads;

		numGenerator = new Random();
		
		position[0] = 0;
		position[1] = 0;
		
		// Creates random initial heading
		heading = numGenerator.nextInt(4);

		System.out.println("Initial Heading: " + heading);
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
		
		System.out.println(String.format("Possible Headings for %d, %d: %s", row, col, possibleHeadings.toString()));
		
		return possibleHeadings;
	}

	@Override
	public int[] getCurrentTrueState() {
		return new int[] {position[0], position[1], heading};
	}

	@Override
	public int[] getCurrentReading() {
		// TODO Auto-generated method stub
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
//		System.out.println("ProbMap: " + probMap.toString());
		
		if (probMap.containsKey(nH) && HEADINGS[nH][0] == dx && HEADINGS[nH][1] == dy) {
				System.out.println("---T PROB ---");
				System.out.println(String.format("(x, y, h) -- (%d, %d, %d)", x, y, h));
				System.out.println(String.format("(nX, nY, nH) -- (%d, %d, %d)", nX, nY, nH));		
				System.out.println("Prob Map: " + probMap.toString());
				return probMap.get(nH);
		}
		
		return 0.0;
	}

}
