package model;

import control.EstimatorInterface;

import java.util.Random;
import java.util.ArrayList;

public class HMMLocalizer implements EstimatorInterface {

	private int rows, cols, headings;
	private int[] position;
	private Random numGenerator;
	
	public HMMLocalizer(int rows, int columns, int headings) {
		this.cols = columns;
		this.rows = rows;
		this.headings = headings;

		// Creates a random starting point for the robot
		numGenerator = new Random();
		int randomX = numGenerator.nextInt(rows);
		int randomY = numGenerator.nextInt(columns);

		// Places robot at random position
		position[0] = randomX;
		position[1] = randomY;
	}
	
	@Override
	public int getNumRows() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumCols() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumHead() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void update() {
		// TODO Auto-generated method stub

	}

	@Override
	public int[] getCurrentTrueState() {
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub
		return 0;
	}

	private boolean isInBounds(int x, int y, int dx, int dy) {
		return (x + dx) >= 0 && (x + dx) < rows && (y + dy) >= 0 && (y + dy) < cols;
	}

}
