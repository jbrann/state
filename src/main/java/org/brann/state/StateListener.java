package org.brann.state;

/**
 * Interface to be implemented by classes that receive callbacks when a state is
 * entered and exited.
 * 
 * @author John.Brann
 * 
 */
public interface StateListener {

	/**
	 * Callback that is triggered whenever the state is entered.  The invocation is synchronous, the state machine will not proceed
	 * until the callback completes.
	 */
	void onEnterState();

	/**
	 * Callback that is triggered whenever the state is left.  The invocation is synchronous, the state machine will not proceed
	 * until the callback completes.
	 */
	void onLeaveState();
}