import lejos.hardware.Sound;
import lejos.hardware.motor.Motor;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3TouchSensor;
import java.io.*;
import java.util.LinkedList;

public class Learn {
	private static int baseStart = -70;
	private static int pivotStart = -170;
	final static int UP = 0;
	final static int NEUTRAL = 1;
	final static int DOWN = 2;
	private static int[] basePosition;
	private static int[] pivotPosition;
	private LinkedList<State> states;
	private EV3TouchSensor touch;

	public static void main(String[] args) throws InterruptedException, IOException {
		basePosition = new int[3];
		pivotPosition = new int[3];
		Motor.A.setSpeed(150);
		Motor.B.setSpeed(150);
		Learn m = new Learn();
	} 

	private void calibrate() {
		basePosition[0] = baseStart;
		pivotPosition[0] = pivotStart;
		for (int i = 1; i < 3; i++) {
			basePosition[i] = basePosition[i - 1] + 30;
			pivotPosition[i] = pivotPosition[i - 1] - 60;
		}
	}
	private void buildStateSpace(){
		states = new LinkedList<State>();
		for(int i = 0; i < 3; i ++)
			for(int j = 0; j < 3; j++)
				for(int k = 0; k < 6; k ++){
					State s = new State(i,j);
					s.action = k;
					states.add(s);
				}
			
		
	}
	public Learn() throws InterruptedException, IOException {
		touch = new EV3TouchSensor(SensorPort.S1);
		int episodes = 20;
		int steps = 50;
		double epsilon = 0.5;
		double alpha = 1;
		double gamma = .9;
		buildStateSpace();
		State current = new State(0,0);
		State next = new State(0,0);
		for(int ep = 0; ep < episodes;ep++){
			// grabs the best start action
			reset();
			calibrate();
			System.out.println("Go");
			double low = -Double.MAX_VALUE;
			for(State s: states)
				if(s.base == 0 && s.pivot == 0 && s.value > low){
					current.action = s.action;
					current.base =0;
					current.pivot=0;
					current.value = s.value;
					low = s.value;
				}
			// decrease alpha and epsilon every 2 episodes
			if(ep % 2 == 0){
				epsilon = epsilon - .1;
				alpha = alpha - .1;
			}
			for(int st = 0; st < steps; st++){
				next = getNextState(current);
				if(Math.random() < epsilon){
					current.action = (int)(Math.random() *6);
					for(State s: states){
						if(s.equals(current)){
							if(current.execute()){
								s.value = s.value+(alpha*(5+(gamma*next.value) - s.value));
							}else{
								s.value = s.value+(alpha*(-1+(gamma*next.value) - s.value));
							}
						}
					}
				}else{
					for(State s: states){
						if(s.equals(current)){
							if(current.execute()){
								s.value = s.value+(alpha*(5+(gamma*next.value) - s.value));
							}else{
								s.value = s.value+(alpha*(-1+(gamma*next.value) - s.value));
							}
						}
					}
				}
				current = next;
			}
			savePolicy();
		}
		Sound.buzz();	
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

	public void savePolicy() throws IOException{
		File f = new File("p.txt"); 
		if(f.exists())
			f.delete();
		f.createNewFile();
		BufferedWriter write = new BufferedWriter(new FileWriter(f));
		for(int i = 0;i < states.size();i++){
			write.write(states.get(i).base+" "+states.get(i).pivot+" "+states.get(i).action+" "+states.get(i).value+ "\n");
		}
		write.flush();
		write.close();
	}
	
	

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

		public boolean execute() throws InterruptedException {
			float[] sample = new float[touch.sampleSize()];
			float pressed;
			touch.fetchSample(sample, 0);
			if (sample[0] == 1) {
				pressed = 1;
			} else {
				pressed = 0;
			}
			if (action < 3) {
				Motor.B.lock(100);
				Motor.A.rotateTo(basePosition[action]);
				Motor.A.lock(100);
				Thread.sleep(100);
				touch.fetchSample(sample, 0);
			} else {
				Motor.A.lock(100);
				Motor.B.rotateTo(pivotPosition[action - 3]);
				Motor.B.lock(100);
				Thread.sleep(100);
				touch.fetchSample(sample, 0);
			}
			if (pressed != sample[0])
				return true;
			return false;
		}
	}
}

