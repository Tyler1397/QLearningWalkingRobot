import lejos.hardware.Sound;
import lejos.hardware.motor.Motor;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3TouchSensor;
import java.io.*;
import java.util.LinkedList;
import java.util.Scanner;
import lejos.hardware.motor.Motor;

public class LearnedPolicy {
	private LinkedList<State> states;
	private static int[] basePosition;
	private static int[] pivotPosition;
	private static int baseStart = -70;
	private static int pivotStart = -170;
	final static int UP = 0;
	final static int NEUTRAL = 1;
	final static int DOWN = 2;
	
	public static void main(String[] args) throws IOException, InterruptedException {
		LearnedPolicy l = new LearnedPolicy();
	}
	
	public LearnedPolicy() throws IOException, InterruptedException{
		states = new LinkedList<State>();
		basePosition = new int[3];
		pivotPosition = new int[3];
		Motor.A.setSpeed(150);
		Motor.B.setSpeed(150);
		readPolicy();
		runPolicy();
	}
	
	private void readPolicy() throws IOException, InterruptedException{
		File f = new File("p.txt");
		BufferedReader read = new BufferedReader(new FileReader(f));
		String line = read.readLine();
		while(line != null){
			Scanner scan = new Scanner(line);
			System.out.println(line);
			int base = scan.nextInt();
			int pivot = scan.nextInt();
			int action = scan.nextInt();
			double value = scan.nextDouble();
			State s = new State(base,pivot);
			s.action = action;
			s.value = value;
			states.add(s);
			line = read.readLine();
		}
		read.close();
	}
	
	private void calibrate() {
		basePosition[0] = baseStart;
		pivotPosition[0] = pivotStart;
		for (int i = 1; i < 3; i++) {
			basePosition[i] = basePosition[i - 1] + 30;
			pivotPosition[i] = pivotPosition[i - 1] - 60;
		}
	}
	private void runPolicy() throws InterruptedException{
		State currentState = new State(UP,UP);
		State nextState;
		reset();
		calibrate();
		//get the best start state 
		double max = -Double.MAX_VALUE;
		for (State s : states)
			if (s.base == 0 && s.pivot == 0){
					currentState = s;
					max = currentState.value;
				}
		
		for(int i = 0; i < 500;i++){
			nextState = getNextState(currentState);
			System.out.println(nextState.base+" "+ nextState.pivot+" "+nextState.action);
			currentState.execute();
			currentState = nextState;
		}
	}
	@SuppressWarnings("deprecation")
	public static void reset() throws InterruptedException {
		Motor.B.flt();
		Motor.A.flt();
		Motor.A.setStallThreshold(50, 100);
		Motor.B.setStallThreshold(50, 100);
		while (!Motor.A.isStalled())
			Motor.A.backward();
		Motor.A.lock(100);
		while (!Motor.B.isStalled())
			Motor.B.forward();
		Motor.B.lock(100);
		baseStart = (int) Motor.A.getPosition();
		pivotStart = (int) Motor.B.getPosition();
		Thread.sleep((1000));
	}
	
	public State getNextState(State st){
		State temp = new State(st.base,st.pivot);
		switch (st.action) {
		case 0:
			temp.base = UP;
			break;
		case 1:
			temp.base = NEUTRAL;
			break;
		case 2:
			temp.base = DOWN;
			break;
		case 3:
			temp.pivot = UP;
			break;
		case 4:
			temp.pivot = NEUTRAL;
			break;
		case 5:
			temp.pivot = DOWN;
			break;
		}
		double low = -Double.MAX_VALUE;
		for(State s: states)
			if(s.base == temp.base && s.pivot == temp.pivot && s.value > low){
				temp.action = s.action;
				temp.value = s.value;
				low = s.value;
			}
		return temp;
	}
	private class State {
		int base, pivot;
		double value;
		int action = 0;

		public State(int base, int pivot) {
			this.base = base;
			this.pivot = pivot;
			value = 0;
			action = (int) (Math.random() * 6);
		}

		public int hashCode() {
			super.hashCode();
			String temp = base + "" + pivot + "" + action;
			return Integer.parseInt(temp);
		}
		@Override
		public boolean equals(Object o) {
			State temp = (State) o;
			if (this.base == temp.base && this.pivot == temp.pivot && this.action == temp.action)
				return true;
			return false;

		}

		@SuppressWarnings("deprecation")
		public void execute() throws InterruptedException {
			if (action < 3) {
				Motor.B.lock(100);
				Motor.A.rotateTo(basePosition[action]);
				Motor.A.lock(100);
				Thread.sleep(100);
			} else {
				Motor.A.lock(100);
				Motor.B.rotateTo(pivotPosition[action - 3]);
				Motor.B.lock(100);
				Thread.sleep(100);
			}
		}
	}
}
