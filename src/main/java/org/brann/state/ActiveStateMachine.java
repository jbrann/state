package org.brann.state;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * The ActiveStateMachine is an autonomous engine that drives the stateMachine
 * machine for the communication connection. It's a bit like an old-fashioned
 * ship's telegraph where someone on the bridge sends a command
 * ("full-steam-ahead" or "all stop") to the engine room, which strives to
 * fulfill the command, and knows the actual state. The ActiveStateMachine
 * navigates from the current actual state according to the directed graph of
 * allowed transitions towards the goal state. Each transition has a name and an
 * end state. The same transition name may be attached to many start states. In
 * addition, the State Machine can be guided by outside Events (as in a more
 * classic state machine implementation) The state machine directed graph can be
 * cyclic - which requires care.
 * 
 * Clients create the graph of states and transitions by making calls to
 * addState() and addTransition().
 * 
 * Clients can register callbacks on states (StateListener) and transitions
 * (TransitionListener). StateListeners receive callbacks when the state is
 * entered and when it is left. Transition listeners are invoked when the
 * transition occurs FROM the identified starting state for the listener. These
 * callbacks can be used to arrest progress through the State graph by blocking
 * until their requirements are satisfied. Multiple callbacks can be registered
 * on a single State or Transition. All will be executed in a single Thread when
 * the appropriate condition becomes true. The system makes no guarantees
 * regarding the order of invocation of callbacks when multiple are triggered on
 * oone Event or transition.
 * 
 * Clients can create and trigger Events. An Event attached to a state will
 * trigger a transition away from the state when the client raises the Event.
 * When a client raises the event, the current state is examined to determine if
 * the event applies to it. if it does, the identified transition is triggered.
 * Client (by calling the event() method). This technique can be used to
 * implement a passive state machine; If the Client does not call start(), the
 * ActiveStateMachine does not drive transitions and Events are the only way of
 * changing the State.
 * 
 * A Client can provide their own Thread to drive the Active State Machine by
 * using "SetStateWait()" which will identify the target end state and drive to
 * it (just as the ActiveStateMachine's own thread would).
 * 
 * @author John.Brann
 * 
 */
public class ActiveStateMachine implements Runnable {

	/**
	 * default StateListener that takes no action on state entry or exit.
	 * 
	 * @author John.Brann
	 * 
	 */
	class nullStateListener implements StateListener {

		public void onEnterState() {
		}

		public void onLeaveState() {
		}
	}

	/**
	 * A step between states (an edge of the directed graph of states). The name
	 * and end state are required at construction time and are immutable.
	 * 
	 * A transition is uniquely identified by the State to which it is attached
	 * and its name.
	 * 
	 * @author John.Brann
	 * 
	 */
	class Transition {
		String name;
		State endState;
		Set<TransitionListener> listeners;

		public Transition(String name, State endState) {
			this.name = name;
			this.endState = endState;
			this.listeners = null;
		}

		public Transition(String name, State endState, TransitionListener listener) {
			this.name = name;
			this.endState = endState;
			this.addListener(listener);
		}

		/**
		 * Add the parameter TransitionListener to the listeners for this
		 * transition. A single Listener object can only be added once
		 * 
		 * @param listener
		 *            The TransactionListener to be added to the
		 *            TransitionListeners on this transition
		 * @return true if added, false if already attached to the transition.
		 */
		public boolean addListener(TransitionListener listener) {

			if (listeners == null) {
				listeners = new HashSet<TransitionListener>();
			}

			if (!listeners.contains(listener)) {
				listeners.add(listener);
				return true;
			} else {
				return false;
			}
		}

		/**
		 * Return the name of this transition
		 * 
		 * @param name
		 */
		public String getName() {
			return name;
		}

		/**
		 * return the state at the end of this Transition
		 * 
		 * @return
		 */
		public State getEndState() {
			return endState;
		}
	}

	/**
	 * A state of the machine (a node of the directed graph)
	 * 
	 * @author John.Brann
	 * 
	 */
	class State {
		String name;
		Set<StateListener> listeners;
		java.util.Map<String, Transition> transitions;
		java.util.Map<String, Transition> events;

		/**
		 * Create a new State with an immutable name.
		 * 
		 * @param name
		 */
		State(String name) {
			this.name = name;
		}

		/**
		 * A "terminal" state is one that has no Transitions leaving it. Once
		 * processing enters such a state, it can never leave (unless the graph
		 * is altered).
		 * 
		 * @return
		 */
		boolean isTerminal() {
			return transitions == null;
		}

		/**
		 * returns the set of StateListeners associated with this State.
		 * 
		 * @return
		 */
		public Set<StateListener> getListener() {
			return listeners;
		}

		/**
		 * Add a StateListener to this State. A single StateListener Object can
		 * only be added once to a single State.
		 * 
		 * @param listener
		 * @return true if the StateListener is successfully added, false
		 *         otherwise
		 */
		public boolean addListener(StateListener listener) {

			if (this.listeners == null) {
				this.listeners = new HashSet<StateListener>();
			}
			if (listeners.contains(listener)) {
				return (false);
			} else {
				listeners.add(listener);
				return true;
			}
		}

		/**
		 * returns true if at least one StateListener is associated with the
		 * State.
		 * 
		 * @return true if there is at least one StateListener for this State,
		 *         false otherwise.
		 */
		public boolean hasListeners() {
			return listeners != null;
		}

		/**
		 * Getter for the name of this State.
		 * 
		 * @return The State's name
		 */
		public String getName() {
			return name;
		}

		/**
		 * Attach a transition leaving this state. Name must be unique among
		 * transitions out of this state and only one transition is permitted to
		 * each other state. [Otherwise the behaviour of the active machine
		 * would be unpredictable - either transition might be chosen].
		 * 
		 * @param name
		 *            The name of the transition
		 * @param endState
		 *            the State to transition to.
		 * @return true if the transition was added, false otherwise
		 */
		public boolean addTransition(String name, State endState) {

			if (transitions == null) {
				transitions = new LinkedHashMap<String, Transition>();
			}
			// can't have two transitions to the same target state
			for (Transition t : transitions.values()) {
				if (t.getEndState() == endState) {
					return false;
				}
			}
			// ok - add the transition
			transitions.put(name, new Transition(name, endState));
			return true;
		}

		/**
		 * Returns the named Transition Object that has been defined as leaving
		 * this State
		 * 
		 * @param name
		 * @return the transition, or false if the named transition does not
		 *         leave this State
		 */
		public Transition getTransition(String name) {

			return (transitions.get(name));

		}

		/**
		 * Add an external Event to the State. The Event will trigger the named
		 * transition. Note that the transition MUST already have been defined.
		 * 
		 * @param name
		 * @param transition
		 * @return true if the Event is attached, false if not.
		 */
		public boolean addEvent(String name, String transitionName) {

			if (transitions == null || !transitions.containsKey(transitionName)) {
				// the specified transition does not exist
				return false;
			}
			if (events == null) {
				events = new LinkedHashMap<String, Transition>();
			}
			events.put(name, transitions.get(transitionName));
			return true;
		}
	}

	/**
	 * An externally fired event that triggers a transition of the state
	 * machine.
	 * 
	 * @author John.Brann
	 * 
	 */
	class Event {
		String name;

		/**
		 * Build a named Event.
		 * 
		 * @param name
		 */
		public Event(String name) {
			this.name = name;
		}

		/**
		 * Getter for the Event name.
		 * 
		 * @return the Ebvent's name
		 */
		public String getName() {
			return name;
		}
	}

	/**
	 * Construct a new ActiveStateMachine that will contain States, transitions
	 * and strive to move to its goal.
	 * 
	 */
	public ActiveStateMachine() {

		init();
	}

	/**
	 * Construct an Active State MAchine, specifying the name of the Thread that
	 * will drive state progress. This can be useful for debugging.
	 * 
	 * @param threadName
	 */
	public ActiveStateMachine(String threadName) {

		init();
		myThread.setName(threadName);
	}

	/**
	 * Initialize the ActiveStateMachine. called from all Constructors.
	 */
	private void init() {
		states = new LinkedHashMap<String, State>();
		myThread = new Thread(this, "asmThread");
		myThread.setDaemon(true);
	}

	/**
	 * Start the ActiveStateMachine towards its goal. if the ActiveStateMachine
	 * has alresy been started, does nothing.
	 */
	public void start() {

		if (!started && initialState != null && !myThread.isAlive()) {

			started = true;
			if (actualState == null) {
				actualState = initialState;
			}
			if (goalState == null) {
				goalState = initialState;
			}

			myThread.start();
		}
	}

	/**
	 * Terminates the Active State Machine. The Thread driving the
	 * ActiveStateMachine is terminated. The States, Transitions and listeners
	 * remain intact. The ActiveStateMachine can be restarted at a later time,
	 * from the (then current) state.
	 * 
	 */
	public void stop() {

		started = false;
		String tName = myThread.getName();

		myThread.interrupt();
		try {
			myThread.join();
		} catch (InterruptedException e) {
			// don't care
		}
		myThread = new Thread(this, tName);
		myThread.setDaemon(true);

	}

	/**
	 * Create a new State. State names must be unique within an
	 * ActiveStateMachine. Requesting a state with the same name as one
	 * previously created returns false;
	 * 
	 * @param name
	 * @return true if the State is added, false otherwise.
	 */
	public synchronized boolean addState(String name) {

		if (states.keySet().contains(name)) {
			return false;
		} else {
			states.put(name, new State(name));
			if (initialState == null)
				initialState = states.get(name);
			return true;
		}
	}

	/**
	 * Add the argument StateListener to the named State of this
	 * ActiveStateMachine. Returns true if the StateListener is added, false
	 * otherwise. Reasons for failure include: 1. The named State does not exist
	 * in this ActiveStateMachine 2. The named State already has the argument
	 * StateListener attached.
	 * 
	 * @param state
	 *            The name of the State to which the listener should be
	 *            attached.
	 * @param listener
	 *            The StateListener to be attached
	 * @return true on success, false otherwise.
	 */
	public synchronized boolean addListener(String state, StateListener listener) {

		if (states.containsKey(state)) {
			return (states.get(state).addListener(listener));
		}
		return false;
	}

	/**
	 * Identifies an event that can occur on a State and associates it with a
	 * Transition to execute. Note that the State must already exist and the
	 * named Transition must have been previously associated with the State.
	 * 
	 * This operation may fail and return false if: - the named State does not
	 * exist in this StateMachine - the named Transition does not leave the
	 * named State (or does not exist)
	 * 
	 * If the named Event was previously added to the named State, this
	 * operation succeeds and replaces the previous Event with this one.
	 * 
	 * @param eventName
	 *            The name of the event being created
	 * @param stateName
	 *            the state on which the event is attached
	 * @param transitionName
	 *            the transition to be executed in response to the event
	 * @return true if the event is correctly attached, false otherwise.
	 */
	public synchronized boolean addEvent(String eventName, String stateName, String transitionName) {

		if (!states.containsKey(stateName) || !states.get(stateName).transitions.containsKey(transitionName)) {
			return false;
		} else {
			return states.get(stateName).addEvent(eventName, transitionName);
		}
	}

	/**
	 * 
	 * Create a Transition
	 * 
	 * @param fromState
	 *            name of starting State for the Transition
	 * @param toState
	 *            name of end State for the Transition
	 * @param name
	 *            Name of the transition
	 * @return true if successfully added, false if either of the transitions
	 *         does not exist, or the fromState already has a Transition of this
	 *         name.
	 */
	public synchronized boolean addTransition(String fromState, String toState, String name) {

		// validate the proposed transition
		State source = states.get(fromState);
		State target = states.get(toState);
		if (source != null && target != null) {
			return source.addTransition(name, target);
		}
		return false;
	}

	/**
	 * 
	 * Shortcut operation to create a Transition and attach the argument
	 * Listener to it.
	 * 
	 * @param fromState
	 *            name of starting State for the Transition
	 * @param toState
	 *            name of end State for the Transition
	 * @param name
	 *            Name of the transition
	 * @param listener
	 *            TransitionListener object to be fired when the Transition
	 *            occurs.
	 * @return true if successfully added, false if either of the transitions
	 *         does not exist, or the fromState already has a Transition of this
	 *         name.
	 */
	public synchronized boolean addTransition(String fromState, String toState, String name,
			TransitionListener listener) {
		Transition t;

		// validate the proposed transition
		State source = states.get(fromState);
		State target = states.get(toState);
		if (source != null && target != null) {
			if (source.addTransition(name, target)) {
				if ((t = source.getTransition(name)) != null) {
					t.addListener(listener);
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * The Thread run() operation that drives the ActiveStateMachine.
	 */
	public void run() {

		// as long as the ActiveStateMachine is not stopped
		// and the state we are in is not terminal
		while (started && !getActualState().isTerminal()) {
			try {
				if (!doNextAction()) {
					// nothing to do - wait around for a while.
					synchronized (this) {
						this.wait(1000);
					}
				}
			} catch (InterruptedException ie) {
				// don't care
			}
		}
	}

	/**
	 * Determine what action needs to be taken, given the current desired and
	 * actual states and do it. returns true if a transition is completed, false
	 * otherwise.
	 * 
	 * The determination of shortest path is brute force (traveling salesman),
	 * so may be expensive for big state charts.
	 * 
	 * @return true if we found a transition to make and made it. false
	 *         otherwise
	 */
	private boolean doNextAction() {

		// If we have reached our goal, or there is nowhere to go, false return.
		if (goalState == actualState || actualState.isTerminal()) {
			return false;
		} else {// iterate over transitions for the one that leads to the target
				// in the fewest steps.
			int stepCount = NO_PATH;
			Transition bestPath = null;
			for (Transition t : actualState.transitions.values()) {

				java.util.Set<State> visited = new java.util.HashSet<State>();
				int steps = walkForTarget(visited, t.endState, goalState);
				if (steps < stepCount) {
					// this is a better path
					bestPath = t;
					stepCount = steps;
				}
			}

			/*
			 * There is a path to the destination Execute the first transition
			 */
			if (bestPath != null) {
				doTransition(bestPath);
				return true;
			}
		}
		return false;
	}

	/**
	 * Synchronously transitions to specified target state. WARNING - may take a
	 * long time, depending on TransitionListener actions.
	 * 
	 * @param targetState
	 *            The state to go to.
	 * @return true on success - current state is target, false if at any point
	 *         progrss to the goal cannot be made.
	 */
	public synchronized boolean setStateWait(String targetState) {

		setGoalState(targetState);

		State ts = states.get(targetState);

		while (doNextAction())
			;

		return (ts == getActualState());
	}

	/**
	 * Recursively explore the State and transition graph from the source State
	 * to the target State, counting the number of steps and (ultimately)
	 * returning the total. If a cycle is detected the process stops, returning
	 * a "No Path" value.
	 * 
	 * @param visited
	 *            a Set of the States visited on this path (starts empty)
	 * @param source
	 *            the current source State in the path
	 * @param target
	 *            the target State of the path
	 * @return a count of the steps in the path or the "No Path" value, if there
	 *         isn't a path
	 */
	int walkForTarget(java.util.Set<State> visited, State source, State target) {

		int bestCount = NO_PATH;

		if (source == target) {
			// we have arrived at the target
			return visited.size();
		}

		if (source.isTerminal()) {
			// we can't get to the target from here
			return NO_PATH;
		}

		if (visited.contains(source)) {
			// circular graph - can't get to target this way
			return NO_PATH;
		}

		visited.add(source);

		// for each transition out of this State, count the steps to target
		// find the minimum and return it.

		for (Transition nextTransition : source.transitions.values()) {
			int thisCount;
			State nextState = nextTransition.getEndState();

			thisCount = walkForTarget(visited, nextState, target);

			if (thisCount < bestCount)
				bestCount = thisCount;
		}
		return bestCount;
	}

	/**
	 * Identify the state that the machine will try to move to.
	 * 
	 * The operation will fail and return false in several circumstances: - The
	 * named State does not exist in this State machine - the named State cannot
	 * be reached from the current State
	 * 
	 * @param targetState
	 *            name of the state that we want to get to
	 * @return true if we successfully set the goal, false otherwise
	 */
	public synchronized boolean setGoalState(String targetState) {

		State target;

		// various reasons for failure
		if (actualState.isTerminal() || (target = states.get(targetState)) == null
				|| (walkForTarget(new HashSet<State>(), actualState, target) == NO_PATH)) {

			return false;
		} else {
			goalState = states.get(targetState);

			if (started) {
				// wake up the ActiveStateMachine thread
				synchronized (this) {
					this.notify();
				}
			}
			return true;
		}
	}

	/**
	 * An external event has been detected that will adjust the actual state.
	 * Note that the event is applied iteratively until it no longer has an
	 * effect. In other words the event causes a graph walk, not just a single
	 * transition.
	 * 
	 * @param eventName
	 *            the name of the event.
	 */
	public synchronized void event(String eventName) {

		while (actualState.events != null && actualState.events.containsKey(eventName)) {
			doTransition(actualState.events.get(eventName));
		}
	}

	/**
	 * What is the current State?
	 * 
	 * @return the current State object
	 */
	synchronized State getActualState() {
		return actualState;
	}

	/**
	 * What is the name of the current State?
	 * @return Name of the current State
	 */
	public synchronized String getActualStateName() {
		return actualState.name;
	}

	/**
	 * What is the name of the current goal State?
	 * @return name of current Goal State, null if no goal has been set
	 */
	public synchronized String getGoalStateString() {
		
		if (goalState == null) {
			return null;
		} else {
			return goalState.name;
		}
	}

	/**
	 * Perform the identified Transition, invoking:
	 * - the Transition callback for all TransitionListeners of the transition
	 * - the exit callback of all StateListeners for the source state
	 * - the entry callback of all StateListeners of the target state.
	 * 
	 * Note that if any of the transition callback returns false, the Transition is NOT executed
	 * and the Event callbacks are NOT invoked
	 * 
	 *  These are invoked in the above order, but no guarantees are made about the 
	 *  sequence of invocation for callbacks of the same type.
	 *  
	 * @param transition - the transition being executed.
	 */
	private void doTransition(Transition transition) {

		// validate that the Transition is valid for the current State
		if (transition != actualState.getTransition(transition.getName())) {
			//nope!
			return;
		}
		
		// invoke the callbacks on the transition
		boolean transitionGood = true;
		if (transition.listeners != null) { 
			for (TransitionListener listener : transition.listeners) {
					if (!listener.onAction()) {
						transitionGood = false;
					}
			}
		}
		
		// a bad transition callback.
		if (!transitionGood) {
			return;
		}
		
		Set<StateListener> sListeners = actualState.getListener();
		
		if (sListeners != null &&
			!sListeners.isEmpty()) {
			
			for (StateListener listener : sListeners) {
				listener.onLeaveState();
			}
		}
		
		// move the actualState to the one at the target end of the transition.
		
		this.actualState = transition.endState;

		if (actualState != null) {
			sListeners = actualState.getListener();
		} else {
			sListeners = null;
		}
		
		// perform the entry callbacks for the new state.
		if (sListeners != null &&
			!sListeners.isEmpty()) {
				
				for (StateListener listener : sListeners) {
					listener.onEnterState();
				}
			}		
	}

	/**
	 * What is the initial state that we started this State Machine from?
	 * 
	 * @return the Initial state of the State Machine
	 */
	public State getInitialState() {
		return initialState;
	}

	/**
	 * Explicitly set the Initial state of the State machine.  This has little effect if the Active State Machine
	 * has already started - it will be returned by a call to getInitialState, though.
	 * 
	 * Setting the initial state does NOT result in any callbacks - 
	 * even to the StateListener callback for entering the State.
	 * 
	 * @param initialState -  the name of the State where the Active State Machine will start.
	 * @return true if the state is set, false otherwise.
	 */
	public boolean setInitialState(String initialState) {
		
		
		if (states.containsKey(initialState)) {
			this.initialState = states.get(initialState);
			return true;
		}
		return false;
			
	}

	/**
	 * What State are we trying to reach?
	 * 
	 * @return Value of property goalState.
	 */
	public State getGoalState() {
		return goalState;
	}

	java.util.Map<String, State> states;

	State goalState;
	State actualState;
	State initialState;
	private Thread myThread;
	private boolean started;

	private static final int NO_PATH = Integer.MAX_VALUE;
}