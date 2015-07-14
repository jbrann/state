# state
Active State Machine - a state machine that drives itself to a goal.

 The ActiveStateMachine is an autonomous engine that drives the stateMachine
 machine for the communication connection. It's a bit like an old-fashioned
 ship's telegraph where someone on the bridge sends a command
 ("full-steam-ahead" or "all stop") to the engine room, which strives to
 fulfill the command, and knows the actual state and the steps necessary to 
 achieve the goal. The ActiveStateMachine
 navigates from the current actual state according to the directed graph of
 allowed transitions towards the goal state, using the shortest (in number of states visited) 
 path. Each transition has a name and an
 end state. The same transition name may be attached to many start states. In
 addition, the State Machine can be guided by outside Events (as in a more
 classic state machine implementation) The state machine directed graph can be
 cyclic - which requires care.
 
 Clients create the graph of states and transitions by making calls to
 addState() and addTransition().
 
 The state machine has an Initial state (where it starts), a Goal state (where it
 is trying to go) and a current state (where it is, now).  When the ActiveStateMachine
 is started, the Current state is set to the Initial state and the machine strives to
 reach the Goal state.  The Current state cannot be directly manipulated by clients
 and the Initial state can only be (meaningfully) set before the State Machine has been
 started.  The Goal state CAN be changed during the operation of the state machine irrespective
 of whether the (previous) oal had been reached.  progress will move from the current state
 to the new goal by the shortest path.
 
  In general, a single ActiveStateMachine can only be used until a terminal state (one with no transitions leaving it) is reached for the first time.
 
 Clients can register callbacks on states (StateListener) and transitions
 (TransitionListener). StateListeners receive callbacks when the state is
 entered and when it is left. Transition listeners are invoked when the
 transition occurs. These
 callbacks can be used to arrest progress through the State graph by blocking
 until their requirements are satisfied. Multiple callbacks can be registered
 on a single State or Transition. All will be executed in a single Thread when
 the appropriate condition becomes true. The system makes no guarantees
 regarding the order of invocation of callbacks when multiple are triggered on
 one Event or transition.
 
 Clients can create and trigger Events. An Event attached to a state will
 trigger a transition away from the state when the client raises the Event.
 When a client raises the event, the current state is examined to determine if
 the event applies to it. if it does, the identified transition is triggered.
 If it does not, the Event is discarded (Events are not queued until the 
 State is current).  This technique can be used to
 implement a passive state machine; If the Client does not call start(), the
 ActiveStateMachine does not drive transitions and Events can be used to 
 change the State.
 
 A Client can provide their own Thread to drive the Active State Machine by
 using "SetStateWait()" which will identify the target end state and drive to
 it (just as the ActiveStateMachine's own thread would).
