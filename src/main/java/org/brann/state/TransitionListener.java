package org.brann.state;

/**
 * Interface to be implemented by classes that receive callbacks that fire during a state
 * transition.
 * 
 * @author John.Brann
 * 
 */
public interface TransitionListener {

	/** 
	 * Operation to be implemented by the TransitionListener.  This will be called when the
	 * associated transition fires.  The invocation is synchronous, the state machine will not proceed
	 * until the callback completes.
	 * 
	 * If the callback returns false, the transition does NOT take place (although all of its listeners will
	 * be invoked) and the state will not change.
	 * 
	 * @return true on success
	 */
	boolean onAction();
}