package org.brann.state;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class ActiveStateMachineTest extends ActiveStateMachine {

	class TransListener implements TransitionListener {

		int transitionCount = 0;
		
		public boolean onAction() {

			++transitionCount;
			return true;
		}

		public int getTransitionCount() {
			return transitionCount;
		}
	}
	
	class StListener implements StateListener {

		int enterCall = 0;
		int leavecall = 0;
		
		public void onEnterState() {
			++enterCall;
			
		}

		public void onLeaveState() {
			++leavecall;
			
		}
		
		int getEnterCallCount() {
			return enterCall;
		}
		
		int getLeaveCallCount() {
			return leavecall;
		}
		
	}
	class BlockingTransListener implements TransitionListener {
	
		int transitionCount = 0;
		
		public boolean onAction() {
	
			++transitionCount;
			try {
				while(true) {
					Thread.sleep(1000l);
				}
			} catch (InterruptedException ie) {
				// don't care, just breaks out
			}
			return true;
		}
	
		public int getTransitionCount() {
			return transitionCount;
		}
	}
	@Test
	public void testStart() {

		// set up simple state machine
		ActiveStateMachine asm = new ActiveStateMachine();
		asm.addState("A");
		asm.addState("B");
		asm.addState("C");
		asm.addState("D");
		assertTrue(asm.addTransition("A", "B", "A2B"));
		assertTrue(asm.addTransition("B", "C", "B2C"));
		assertTrue(asm.addTransition("C", "D", "C2D"));
		
		assertTrue(asm.setGoalState("C"));  // should be good, because there is a route from A to C (even though none
		// InitialState and current state default to A:
		assertTrue(asm.getInitialState().getName().compareTo("A") == 0);
		assertTrue(asm.getActualState().getName().compareTo("A") == 0);

		// run the active state machine
		asm.start();
		
		// try to ensure that the thread runs
		Thread.yield();
		try {
			Thread.sleep(2000l);
		} catch (InterruptedException e) {
			// don't care
		}
		
		// the thread should have got us to C
		assertTrue(asm.getActualStateName().compareTo("C") == 0);
		
		asm.stop(); // tidy up thread
			
	}

	@Test
	public void testStop() {
		// set up simple state machine
		ActiveStateMachine asm = new ActiveStateMachine("testStop");
		asm.addState("A");
		asm.addState("B");
		asm.addState("C");
		asm.addState("D");
		assertTrue(asm.addTransition("A", "B", "A2B"));
		assertTrue(asm.addTransition("B", "C", "B2C", new BlockingTransListener()));
		assertTrue(asm.addTransition("C", "D", "C2D"));
		
		assertTrue(asm.setGoalState("D"));  
		// InitialState and current state default to A:
		assertTrue(asm.getInitialState().getName().compareTo("A") == 0);
		assertTrue(asm.getActualState().getName().compareTo("A") == 0);

		// run the active state machine
		asm.start();
		
		// try to ensure that the thread runs
		Thread.yield();
		try {
			Thread.sleep(100l);
		} catch (InterruptedException e) {
			// don't care
		}
		
		// at this point the thread should have got us to B and be blocked on the B->C transition
		assertTrue(asm.getActualStateName().compareTo("B") == 0);
		
		// This will unblock and stop the thread (the transition succeeds because the blocker returns true
		asm.stop();
		
		// should still be at C
		assertTrue(asm.getActualStateName().compareTo("C") == 0);
		
		//goal sate still D
		assertTrue(asm.getGoalStateString().compareTo("D") == 0);
		
		// stop() was impactful because we can restart and the thread will push on to D 
		// run the active state machine
		asm.start();
		
		// try to ensure that the thread runs
		Thread.yield();
		try {
			Thread.sleep(2000l);
		} catch (InterruptedException e) {
			// don't care
		}
	
		// now we reached D
		assertTrue(asm.getActualStateName().compareTo("D") == 0);
		
		
		

		
	}

	@Test
	public void testAddState() {
		ActiveStateMachine asm = new ActiveStateMachine();
		assertTrue(asm.addState("foo"));
		assertTrue((asm.getStates().containsKey("foo")));
		assertTrue(asm.addState("bar"));
		assertTrue((asm.getStates().containsKey("foo")));
		assertTrue((asm.getStates().containsKey("bar")));
		assertFalse(asm.addState("foo"));
	}

	@Test
	public void testAddListener() {

		ActiveStateMachine asm = new ActiveStateMachine("startTest");
		assertTrue(asm.addState("foo"));
		assertTrue(asm.addState("bar"));
		assertTrue(asm.addTransition("foo", "bar", "fooBar"));
		
		StListener myListener = new StListener();
		myListener.onEnterState();  // add 1 to the enter state counter
		assertTrue(myListener.getEnterCallCount() == 1 && //
				   myListener.getLeaveCallCount() == 0);
		
		assertTrue(asm.addListener("foo", myListener));
		assertFalse(asm.addListener("foo", myListener));  // can't add the same listener twice
		assertTrue(asm.addListener("foo", new StListener())); // can add more than 1...
		
		assertFalse(asm.addListener("quux", myListener)); // state doesn't exist
		
		asm.setInitialState("foo");
		
		asm.setStateWait("bar");
		
		assertTrue(myListener.getEnterCallCount() == 1 &&  // state left, but not entered again
				   myListener.getLeaveCallCount() == 1);
		
		
	}

	@Test
	public void testAddEvent() {
		
		ActiveStateMachine asm = new ActiveStateMachine();
		asm.addState("foo");
		asm.setInitialState("foo");
		asm.addState("bar");
		asm.addTransition("foo", "bar", "fooBar");
		assertFalse(asm.addEvent("itsFoo", "bar", "fooBar")); // State doesn't have this transition leaving it
		assertFalse(asm.addEvent("itsFoo", "foo", "fooQuux")); // State doesn't have this transition leaving it
		assertTrue(asm.addEvent("itsFoo", "foo", "fooBar")); // good
		assertTrue(asm.addEvent("itsFoo", "foo", "fooBar"));  // duplicate replaces previous
		
		StListener fListener = new StListener();
		asm.addListener("foo", fListener);
		StListener bListener = new StListener();
		asm.addListener("bar", bListener);
		
		// fire the event, should move us from foo to bar
		
		asm.event("itsFoo");
		
		// should have transitioned from foo to bar, adding 1 to leaving foo counter and 1 to entering bar counter
		assertTrue (fListener.getEnterCallCount() == 0 &&
				    fListener.getLeaveCallCount() == 1 &&
				    bListener.getEnterCallCount() == 1 &&
				    bListener.getLeaveCallCount() == 0);
		
	}

	@Test
	public void testAddTransitionStringStringStringTransitionListener() {

		ActiveStateMachine asm = new ActiveStateMachine();
		asm.addState("foo");
		asm.setInitialState("foo");
		asm.addState("bar");
		
		TransListener myTransitionListener = new TransListener();
		assertTrue(asm.addTransition("foo", "bar", "foo2bar", myTransitionListener));
		assertFalse(asm.addTransition("foo", "bar", "secondTransition", myTransitionListener));  // can't add a second transition
		assertFalse(asm.addTransition("foo", "XXX", "bogusTransition", myTransitionListener));  // can't add a transition to an undefined state
		assertFalse(asm.addTransition("XXX", "bar", "boogusTransition", myTransitionListener));  // can't add a transition from an undefined state
		assertTrue(myTransitionListener.getTransitionCount() == 0);
		
		asm.addEvent("itsFoo", "foo", "foo2bar"); // good
		asm.event("itsFoo");
		
		// should have transitioned and updated the transition counter
		assertTrue(myTransitionListener.getTransitionCount() == 1);
	}

	@Test
	public void testWalkForTarget() {
		
		// set up state map, note F and G unreachable from A-E
		ActiveStateMachine asm = new ActiveStateMachine();
		asm.addState("A");
		asm.addState("B");
		asm.addState("C");
		asm.addState("D");
		asm.addState("E");
		asm.addState("F");
		asm.addState("G");
		assertTrue(asm.addTransition("A", "B", "A2B"));
		assertTrue(asm.addTransition("B", "A", "B2A"));
		assertTrue(asm.addTransition("A", "E", "A2E"));
		assertTrue(asm.addTransition("E", "A", "E2A"));
		assertTrue(asm.addTransition("A", "D", "A2D"));
		assertTrue(asm.addTransition("D", "A", "D2A"));
		assertTrue(asm.addTransition("B", "C", "B2C"));
		assertTrue(asm.addTransition("C", "B", "C2B"));
		assertTrue(asm.addTransition("B", "D", "B2D"));
		assertTrue(asm.addTransition("D", "B", "D2B"));
		assertTrue(asm.addTransition("F", "G", "F2G"));
		assertTrue(asm.addTransition("G", "F", "G2F"));
		assertTrue(asm.addTransition("G", "D", "G2D"));
		
		asm.setInitialState("A");
		
		Set<ActiveStateMachine.State> visited = new HashSet<ActiveStateMachine.State>();
		//Best path A->C is first step B
		assertTrue(walkForTarget(visited, asm.getStates().get("A"), asm.getStates().get("C")).getName().compareTo("A2B") == 0);

		//Best path D->C is first step B
		visited.clear();
		assertTrue(walkForTarget(visited, asm.getStates().get("D"), asm.getStates().get("C")).getName().compareTo("D2B") == 0);

		//Best path D->E is first step A
		visited.clear();
		assertTrue(walkForTarget(visited, asm.getStates().get("D"), asm.getStates().get("E")).getName().compareTo("D2A") == 0);

		//Best path F->E is first step G
		visited.clear();
		assertTrue(walkForTarget(visited, asm.getStates().get("F"), asm.getStates().get("E")).getName().compareTo("F2G") == 0);

		//Best path D->D is null - we are already there
		visited.clear();
		assertTrue(walkForTarget(visited, asm.getStates().get("D"), asm.getStates().get("D")) == null);

		//Best path E->F is null - no path
		visited.clear();
		assertTrue(walkForTarget(visited, asm.getStates().get("E"), asm.getStates().get("F")) == null);

	}

	@Test
	public void testSetGoalState() {

		ActiveStateMachine asm = new ActiveStateMachine();
		asm.addState("A");
		asm.addState("B");
		// InitialState and current state default to A:
		assertTrue(asm.getInitialState().getName().compareTo("A") == 0);
		assertTrue(asm.getActualState().getName().compareTo("A") == 0);
		
		//Hasn't been set - no default
		assertTrue(asm.getGoalState() == null);
		assertTrue(asm.getGoalStateString() == null);
		
		assertFalse(asm.setGoalState("A"));  // can't set to anything when current state is terminal (has no transitions out)
		assertFalse(asm.setGoalState("B"));  // can't set to anything when current state is terminal (has no transitions out)
		
		// add a transition so this makes sense
		assertTrue(asm.addTransition("A", "B", "A2B"));
		
		assertFalse(asm.setGoalState("A"));  // A is no longer terminal, still can't set this because we are already there
		assertTrue(asm.setGoalState("B"));  // should be good
	
		asm.addState("C");
		assertTrue(asm.addTransition("A", "C", "A2C"));
		assertTrue(asm.setGoalState("C"));  // should be good, because there is a route from A to C (even though none
		                                    // from B to C
	}

	@Test
	public void testEvent() {

		ActiveStateMachine asm = new ActiveStateMachine("An unusual thread Name");
		asm.addState("A");
		asm.addState("B");
		asm.addState("C");
		
		// add transitions
		assertTrue(asm.addTransition("A", "B", "A2B"));
		assertTrue(asm.addTransition("B", "C", "B2C"));
		
		// add events
		assertTrue(asm.addEvent("eventA", "A", "A2B"));
		assertTrue(asm.addEvent("eventB", "B", "B2C"));
		
		// start at A - default
		assertTrue(asm.getActualState().getName().compareTo("A") == 0);
		
		// eventB has no effect here
		asm.event("eventB");
		assertTrue(asm.getActualState().getName().compareTo("A") == 0);

		// eventA moves us to state B
		asm.event("eventA");
		assertTrue(asm.getActualState().getName().compareTo("B") == 0);
		
		// eventA again has no effect
		asm.event("eventA");
		assertTrue(asm.getActualState().getName().compareTo("B") == 0);
		
		// eventB moves us to state C
		asm.event("eventB");
		assertTrue(asm.getActualState().getName().compareTo("C") == 0);
		
		//reset to test multi-step event
		asm = new ActiveStateMachine();
		asm.addState("A");
		asm.addState("B");
		asm.addState("C");
		
		// add transitions
		assertTrue(asm.addTransition("A", "B", "A2B"));
		assertTrue(asm.addTransition("B", "C", "B2C"));
		
		// add events
		assertTrue(asm.addEvent("event", "A", "A2B"));
		assertTrue(asm.addEvent("event", "B", "B2C"));

		// default start state is A
		assertTrue(asm.getActualState().getName().compareTo("A") == 0);
		assertTrue(asm.getActualStateName().compareTo("A") == 0);

		asm.event("event");
		
		//event moves us A->B->C
		assertTrue(asm.getActualState().getName().compareTo("C") == 0);
		assertTrue(asm.getActualStateName().compareTo("C") == 0);

		
		

		
	}

	@Test
	public void testSetGetInitialState() {

		ActiveStateMachine asm = new ActiveStateMachine();
		
		// no states defined, initial is null
		assertTrue(asm.getInitialState() == null);
		
		asm.addState("A");
		// first state defined defaults to initial
		assertTrue(asm.getInitialState().getName().compareTo("A") == 0);
		
		asm.addState("B");
		// first state defined defaults to initial doesn't change when more added
		assertTrue(asm.getInitialState().getName().compareTo("A") == 0);
		
		// can set initial state to any defined state, irrespective of connectivity
		assertTrue(asm.setInitialState("B"));
		assertTrue(asm.getInitialState().getName().compareTo("B") == 0);
		
		// can't set to an undefined state
		assertFalse(asm.setInitialState("X"));
		assertTrue(asm.getInitialState().getName().compareTo("B") == 0);
	}


}
