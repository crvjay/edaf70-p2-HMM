package model;
import java.util.Random;
import java.util.ArrayList;


import control.EstimatorInterface;

public class HMMImplementation implements EstimatorInterface {
		
	private int rows, cols, headings;
	private int[] position;
	private Random numGenerator;


	public HMMImplementation( int rows, int columns, int headings) {
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



		public int getNumColumns(){
			return columns;
		}

		public int getNumRows(){
			return rows;
		}

		public int getNumHeadings(){
			return 4;
		}

		public double getTProb(int x, int y, int h, int nX, int nY, int nH){
			int 

		}
		public double getTProb( int x, int y, int h, int nX, int nY, int nH) {
			// implement
		 

		}
		public double getOrXY( int rX, int rY, int x, int y, int h) {
			//implement

		
		}


		public int[] getCurrentTrueState() {
			//implement

		}

		public int[] getCurrentReading() {
			return currentHeading;
		
		}


		public double getCurrentProb( int x, int y) {
			//implement
		}
	
		public void update() {
			//implement
		}


		// Implement Forward Filtering 
		private void forwardFiltering()

	}
	
	private boolean isInBounds(int x, int y, int dx, int dy) {
		return (x + dx) >= 0 && (x + dx) < rows && (y + dy) >= 0 && (y + dy) < columns;
	}
}

	
	
