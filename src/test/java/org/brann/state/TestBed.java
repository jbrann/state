package org.brann.state;

public class TestBed {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		TestBed t = new TestBed();
		t.test();

	}
	
	class TbTransitionListener implements TransitionListener {

		public TbTransitionListener(String name) {
			this.name = name;
		}
		public boolean onAction() {
			System.out.println("Doing Transition:" + name);
			return true;
		}
		String name;
	}
	class TbStateListener implements StateListener {

		TbStateListener (String name) {
			listenerName = name;
		}
		
		public void onEnterState() {
			System.out.println("Entering State:" + listenerName);
		}

		public void onLeaveState() {
			System.out.println("Leaving State:" + listenerName);
		}
		
		String listenerName;
	}
	
	private TestBed() {
		
		asm = new ActiveStateMachine();
	}
	
	void test() {
		
		/* Simple test case - 4 states a,b,c,d
		 * a is initial
		 * d is final
		 * a -> b
		 * a -> c
		 * c -> a
		 * b -> a
		 * b -> d
		 * 
		 * e->f
		 * f->g
		 * 
		 * transition names are:  <state>To<state>  (e.g. aTob)
		 * 
		 */
		
		ActiveStateMachine.State s;
		String sa [] = { "a", "b", "c", "d", "e", "f", "g" };
		
		// build states 
		for (int i=0;i<sa.length;++i) {
			
			System.out.println(sa[i] + " " + asm.addState(sa[i]));
			asm.addListener(sa[i], new TbStateListener(sa[i]));
		}
		
		//build the transitions
		
		asm.addTransition("a", "c", "aToc", new TbTransitionListener("aToc"));
		asm.addTransition("a", "b", "aTob", new TbTransitionListener("aTob"));
		asm.addTransition("c", "a", "cToa", new TbTransitionListener("cToa"));
		//asm.addTransition("c", "b", "cTob", new TbTransitionListener("cTob"));
		asm.addTransition("b", "d", "bTod", new TbTransitionListener("bTod"));
		asm.addTransition("b", "a", "bToa", new TbTransitionListener("bToa"));
		asm.addTransition("e", "f", "eTof", new TbTransitionListener("eTof"));
		asm.addTransition("f", "g", "fTog", new TbTransitionListener("fTog"));
		
//		asm.setInitialState("d");
		asm.start();
		asm.start();
		
		System.out.println ("Setting end to e - should be false:" + asm.setGoalState("e")); // should refuse - no way to get there
		System.out.println ("Setting end to c - should be true:" + asm.setGoalState("c"));
		Thread.yield();
		asm.setGoalState("d");
		Thread.yield();
		
		
	}

	ActiveStateMachine asm;
}
