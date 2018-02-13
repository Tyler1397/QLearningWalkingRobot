// Author: Tyler Timm
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

public class Main {
	static int[][] maze;
	static final int UP = 0;
	static final int DOWN = 1;
	static final int LEFT = 2;
	static final int RIGHT = 3;
	static int iterations;
	static ArrayList<Integer> targets;
	static int numTargets;
	static ArrayList<State> stateSet;

	public static void main(String[] args) throws FileNotFoundException {
		// scan over file, save the total number of targets to a global variable
		// and create an integer array representation of the maze
		@SuppressWarnings("resource")
		Scanner scan = new Scanner(new File(args[0]));
		numTargets = Integer.parseInt(args[1]);
		maze = new int[scan.nextInt()][scan.nextInt()];
		targets = new ArrayList<Integer>();
		// load maze into an Integer array and grab the targets
		for (int row = 0; row < maze.length; row++)
			for (int column = 0; column < maze[row].length; column++) {
				maze[row][column] = scan.nextInt();
				if (maze[row][column] > 1)
					targets.add(maze[row][column]);
			}
		Collections.sort(targets);
		while (numTargets != targets.size())
			targets.remove(0);

		@SuppressWarnings("unused")
		Main m = new Main();
	}

	public Main() {
		stateSet = new ArrayList<State>();
		for (int row = 0; row < maze.length; row++)
			for (int col = 0; col < maze[row].length; col++) {
				State state = new State(row, col);
				getPowerSet(state);
			}
		System.out.println("Testing Deterministic MDP."+"\n");
		System.out.println("Targets to obtain: "+targets.toString()+" \n ");
		System.out.println("Doing policy iteration... ");
		iterations = 0;
		updatePolicy();
		System.out.println("     Done.");
		System.out.println("Evaluated and updated "+iterations+" policies");
		int pathLength = 0;
		String path = "";
		for (int row = 0; row < maze.length; row++)
			for (int column = 0; column < maze[row].length; column++) {
				if (maze[row][column] == 1){
					State start = new State(row,column);
					for(State s : stateSet){
						if(start.row == s.row && start.col == s.col && s.items.size() == start.items.size()){
							start.action = s.action;
						}
					}
					State t;
					while(start.items.size() != targets.size()){
						t = null;
						if(start.action == UP){
							t = transition(start,start.row-1,start.col);
							path = path+" up";
						}
						if(start.action == DOWN){
							t = transition(start,start.row+1,start.col);
							path = path+" down";
						}
						if(start.action == LEFT){
							t = transition(start,start.row,start.col-1);
							path = path+" left";
						}
						if(start.action == RIGHT){
							t = transition(start,start.row,start.col+1);
							path = path+" right";
						}
						pathLength++;
						start = t;
					}
				}
			}
		System.out.println("Path of length "+pathLength+": ["+path+"]");
	}

	private void getPowerSet(State s) {
		int max = (int) (Math.pow(2, targets.size()));
		for (int i = 0; i < max; i++) {
			ArrayList<Integer> subset = new ArrayList<>();
			String bin = Integer.toBinaryString(i);
			while (bin.length() != targets.size())
				bin = "0" + bin;
			for (int b = 0; b < bin.length(); b++)
				if (bin.charAt(b) == '1')
					subset.add(targets.get(b));
			State temp = new State(s.row, s.col);
			temp.items = subset;
			stateSet.add(temp);
		}
	}

	public class State {
		public int row, col, action;
		public ArrayList<Integer> items;
		public double valueFunction;

		public State(int row, int col) {
			this.row = row;
			this.col = col;
			items = new ArrayList<Integer>();
			valueFunction = 0.0;
			action = (int) (Math.random() * 4);
		}

		public String printState() {
			return "Row " + row + " Col " + col + " " + items.toString() + " Value: " + valueFunction + " Action: "
					+ action + " Hashcode: " + this.hashCode();
		}

		public int hashCode() {
			super.hashCode();
			Collections.sort(this.items);
			String temp = "";
			for (Integer i : this.items) {
				temp = i + temp;
			}
			String s = 1 + "" + row + "" + col + "" + temp;
			return Integer.parseInt(s);
		}
	}

	public void evaluatePolicy() {
		double delta = 10;
		double theta = 0.1;
		while (delta >= theta) {
			delta = 0.0;
			double oldVal = 0.0;
			double newVal = 0.0;
			double difference = 0.0;
			for (State s : stateSet) {

				newVal = rewardFunction(s);
				oldVal = s.valueFunction;

				s.valueFunction = newVal;
				difference = Math.abs(newVal - oldVal);
				delta = Math.max(delta, difference);
			}
		}
	}

	public double rewardFunction(State s) {
		// returns 0 if absorbing state, if it hits a wall or the edge of the
		// grid return -1, else return -1 + (.9)*next state value
		if (s.items.size() == targets.size())
			return 0;

		switch (s.action) {
		case UP:
			if (s.row - 1 >= 0) {
				if (maze[s.row - 1][s.col] == -1)
					return -1;
				return -1 + (0.9 * transitionFunction(s, s.row - 1, s.col));
			}
		case DOWN:
			if (s.row + 1 < maze.length) {
				if (maze[s.row + 1][s.col] == -1)
					return -1;
				return -1 + (0.9 * transitionFunction(s, s.row + 1, s.col));
			}
		case LEFT:
			if (s.col - 1 >= 0) {
				if (maze[s.row][s.col - 1] == -1)
					return -1;
				return -1 + (0.9 * transitionFunction(s, s.row, s.col - 1));
			}
		case RIGHT:
			if (s.col + 1 < maze[0].length) {
				if (maze[s.row][s.col + 1] == -1)
					return -1;
				return -1 + (0.9 * transitionFunction(s, s.row, s.col + 1));
			}
		}
		return -1;
	}

	@SuppressWarnings("unchecked")
	private double transitionFunction(State s, int row, int col) {
		// if the next state is sitting on top of a target, get the next state
		// value of the
		// same location holding the new item
		if (targets.contains(maze[s.row][s.col]) && !s.items.contains(maze[s.row][s.col])) {
			State temp = new State(s.row, s.col);
			temp.items = (ArrayList<Integer>) s.items.clone();
			temp.items.add(maze[s.row][s.col]);
			for (State state : stateSet) {
				if (state.hashCode() == temp.hashCode()) {
					return state.valueFunction;
				}
			}
		}
		// if the state moves over an object that it isnt holding, get the new
		// state
		// and return the value
		State temp = new State(row, col);
		temp.items = (ArrayList<Integer>) s.items.clone();
		if (targets.contains(maze[row][col]) && !s.items.contains(maze[row][col])) {
			temp.items.add(maze[row][col]);
			for (State state : stateSet) {
				if (state.hashCode() == temp.hashCode()) {
					return state.valueFunction;
				}
			}
		}
		for (State state : stateSet)
			if (state.hashCode() == temp.hashCode()) {
				return state.valueFunction;
			}
		return 0;
	}

	private void updatePolicy() {
		boolean changed = true;
		while (changed) {
			iterations++;
			evaluatePolicy();
			changed = false;
			for (State s : stateSet) {
				double[] temp = getNeighbors(s);
				double max = -Double.MAX_VALUE;
				int pos = 0;
				for (int i = 0; i < temp.length; i++)
					if (temp[i] > max) {
						pos = i;
						max = temp[i];
					}
				if(pos != s.action){
					s.action = pos;
					changed = true;
				}
			}
		}
	}

	public double[] getNeighbors(State s) {
		double[] temp = new double[4];
		// top neighbor
		if (s.row - 1 >= 0) {
			if (maze[s.row - 1][s.col] == -1)
				temp[0] = -Integer.MAX_VALUE;
			else
				temp[0] = transitionFunction(s, s.row - 1, s.col);
		} else {
			temp[0] = -Integer.MAX_VALUE;
		}
		// bottom neighbor
		if (s.row + 1 < maze.length) {
			if (maze[s.row + 1][s.col] == -1)
				temp[1] = -Integer.MAX_VALUE;
			else
				temp[1] = transitionFunction(s, s.row + 1, s.col);
		} else {
			temp[1] = -Integer.MAX_VALUE;
		}
		// left neighbor
		if (s.col - 1 >= 0) {
			if (maze[s.row][s.col - 1] == -1)
				temp[2] = -Integer.MAX_VALUE;
			else
				temp[2] = transitionFunction(s, s.row, s.col - 1);
		} else {
			temp[2] = -Integer.MAX_VALUE;
		}
		// right neighbor
		if (s.col + 1 < maze[0].length) {
			if (maze[s.row][s.col + 1] == -1)
				temp[3] = -Integer.MAX_VALUE;
			else
				temp[3] = transitionFunction(s, s.row, s.col + 1);
		} else {
			temp[3] = -Integer.MAX_VALUE;
		}
		return temp;
	}
	
	private State transition(State s, int row, int col) {
		// if the next state is sitting on top of a target, get the next state
		// value of the
		// same location holding the new item
		if (targets.contains(maze[s.row][s.col]) && !s.items.contains(maze[s.row][s.col])) {
			State temp = new State(s.row, s.col);
			temp.items = (ArrayList<Integer>) s.items.clone();
			temp.items.add(maze[s.row][s.col]);
			for (State state : stateSet) {
				if (state.hashCode() == temp.hashCode()) {
					return state;
				}
			}
		}
		// if the state moves over an object that it isnt holding, get the new
		// state
		// and return the value
		State temp = new State(row, col);
		temp.items = (ArrayList<Integer>) s.items.clone();
		if (targets.contains(maze[row][col]) && !s.items.contains(maze[row][col])) {
			temp.items.add(maze[row][col]);
			for (State state : stateSet) {
				if (state.hashCode() == temp.hashCode()) {
					return state;
				}
			}
		}
		for (State state : stateSet)
			if (state.hashCode() == temp.hashCode()) {
				return state;
			}
		return null;
	}
}
