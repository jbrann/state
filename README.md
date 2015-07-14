# state
Active State Machine - a state machine that drives itself to a goal.

 The ActiveStateMachine is an autonomous engine. It's a bit like an old-fashioned
 ship's telegraph where someone on the bridge sends a command
 ("full-steam-ahead" or "all stop") to the engine room, which strives to
 fulfill the command, and knows the actual state. The ActiveStateMachine
 navigates from the current actual state according to the directed graph of
 allowed transitions towards the goal state, using the shortest path if more than 
 one is available. Each transition has a name and an
 end state. The same transition name may be attached to many start states, but each such 
 transition is different. In addition, the State Machine can be guided by outside Events (as in a more
 classic state machine implementation) The state machine directed graph can be
 cyclic - which requires care.  The active engine ignores cyclic paths (unless the path to its goal ends before
 the cycle is complete).
 
 Clients create the graph of states and transitions by making calls to
 addState() and addTransition().
 
 Clients can register callbacks on states (StateListener) and transitions
 (TransitionListener). StateListeners receive callbacks when the state is
 entered and when it is left. Transition listeners are invoked when the
 transition occurs FROM the identified starting state for the listener. These
 callbacks can be used to arrest progress through the State graph by blocking
 until their requirements are satisfied. Multiple callbacks can be registered
 on a single State or Transition. All will be executed in a single Thread when
 the appropriate condition becomes true. The system makes no guarantees
 regarding the order of invocation of callbacks when multiple are triggered on
 oone Event or transition.
 
 Clients can create and trigger Events. An Event attached to a state will
 trigger a transition away from the state when the client raises the Event.
 When a client raises the event, the current state is examined to determine if
 the event applies to it. if it does, the identified transition is triggered.
 Client (by calling the event() method). This technique can be used to
 implement a passive state machine; If the Client does not call start(), the
 ActiveStateMachine does not drive transitions and Events are the only way of
 changing the State.
 
 A Client can provide their own Thread to drive the Active State Machine by
 using "SetStateWait()" which will identify the target end state and drive to
 it (just as the ActiveStateMachine's own thread would).
