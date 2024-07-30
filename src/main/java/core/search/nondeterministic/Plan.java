package core.search.nondeterministic;

import java.util.LinkedList;
import java.util.List;

/**
 * Represents a solution plan for an AND-OR search; according to page 135
 * AIMA3e, the plan must be "a subtree that (1) has a goal node at every leaf,
 * (2) specifies one action at each of its OR nodes, and (3) includes every
 * outcome branch at each of its AND nodes." As demonstrated on page 136, this
 * plan can be implemented as a sequence of two steps where the first
 * is an action (corresponding to one OR node) and the second is a list
 * of if-state-then-plan statements (corresponding to an AND node). Here, we use a
 * list of action steps instead of just one action. This allows to simplify conditioned
 * steps with just one if-statement and supports a clean representation of empty plans.
 *
 * @author Ruediger Lunde
 * @author Andrew Brown
 */
public class Plan<S, A> { //extends jason.asSyntax.Plan {

    private static final long serialVersionUID = 1L;

    private List<A> actionSteps = new LinkedList<>();

	private List<IfStatement<S, A>> ifStatements = new LinkedList<>();
    private boolean loop;
    private int step;
    private Plan<S, A> contingencyPlan;
    private Plan<S, A> parent;

    public Plan(boolean loop) {
	this.loop = loop;
	this.step = 0;
	this.contingencyPlan = null;
	this.parent = null;
    }

    public Plan() {
	this(false);
    }

    public void setParent(Plan<S, A> plan) {
	parent = plan;
    }

    public boolean isLoop() {
	return this.loop;
    }
    public boolean isEmpty() {
	return actionSteps.isEmpty() && ifStatements.isEmpty();
    }

	public List<IfStatement<S, A>> getIfStatements() {
		return ifStatements;
	}

    /** Returns the number of steps of this plan. */
    public int size() {
	return ifStatements.isEmpty() ? actionSteps.size() : actionSteps.size() + 1;
    }

    /**
     * Checks whether the specified step (between 0 and size()-1) is an action step or
     * a conditional step.
     */
    public boolean isActionStep(int step) {
	return step < actionSteps.size();
    }

    /** Returns the corresponding action for the given action step. */
    public A getAction(int step) {
	return actionSteps.get(step);
    }

    /**
     * Evaluates the specified conditional step and returns a plan of the first if-statement which matches
     * the given state.
     * @param step A conditional step (last step in the plan).
     * @param state The state to be matched.
     * @return A plan or null if no match was found.
     */
    public Plan<S, A> getPlan(int step, S state) {
	if (isActionStep(step) || step != actionSteps.size())
	    throw new IllegalArgumentException("Specified step is not conditional.");
	for (IfStatement<S, A> ifStatement : ifStatements) {
	    if (ifStatement.testCondition(state))
		return ifStatement.getPlan();
	}
	return null; // no matching plan found for the given state.
    }

    public void setUpParentPlans() {
	for (IfStatement<S, A> ifStatement : ifStatements) {
	    ifStatement.getPlan().setParent(this);
	    ifStatement.getPlan().setUpParentPlans();
	}
    }

    public Plan<S, A> getParentPlan(S state)  {
	// We need to go from top to bottom.
	if (parent == null) {
	    Plan<S, A> plan = this.getPlan(this.actionSteps.size(), state);
	    if(plan != null && plan.size() != 0)
		return plan;

	    return null;
	}
	else {
	    Plan<S, A> plan = parent.getParentPlan(state);
	    if (plan == null)
		return getPlan(actionSteps.size(), state);
	    return plan;
	}
    }

    /** 
     * Resets the entire plan and all the children of the plan.
     */
    public void reset() {
	// Only nullify if the object is not a contingency plan of itself.
	contingencyPlan = null;
	step = 0;
	
	// Reset all the children on the way down.
	for (IfStatement<S, A> ifStatement : ifStatements) {
	    ifStatement.getPlan().reset();
	}
    }
    
    /**
     * !!!WARNING!!! This recursive method will return null after reaching the 
     * goal state or failing. Therefore, it is up to the wrapper/caller of the
     * method to halt at the goal state and to test the goal state condition.
     *
     * @param state The state to be matched.
     * @return An action.
     */
    public A getNextAction(S state) {
	// If we are still in the action increment the step
	// and return the last action.
	if (isActionStep(step)) {
	    return getAction(step++);
	}

	// if statement needs to be evaluated then action executed.
	else {
	    Plan<S, A> plan = getPlan(step, state);

	    // If the plan for the case is empty it means we need
	    // to loop to the oldest parent plan (that is equivalent to
	    // the case of the one we have) to get its actions. There should
	    // be no contingency plan that was already opened for this
	    // object. Otherwise, we are not properly traversing the plan tree.
	    //
	    // The plan should be checked to see if it is null. If so,
	    // it shall return with failure (caught inside the else statement).
	    if (plan != null && plan.isEmpty() && plan.isLoop()
		&& contingencyPlan == null) {

		// If the parent of the current plan is null and we are in a loop
		// condition, it can only be the case that the current plan contains
		// the actions for this loop.
		if (parent == null) {
		    contingencyPlan = this;
		}
		else {
		    contingencyPlan = plan.getParentPlan(state);
		}

		// Save reference to the contingency plan before resetting
		// the entire contingencyPlan that will nullify the object
		// variable contingencyPlan.
		Plan<S, A> tempPlan = contingencyPlan;

		// Reset the entire branch from the contingencyPlan,
		// this is so we loop through all the plan again.
		contingencyPlan.reset();

		// Return the first action from the plan that the loop
		// was referencing.
		return tempPlan.getNextAction(state);
	    }


	    // The plan exist and therefore we need to recurse.
	    else {
		// Save reference to the plan that we are in if we
		// are not already in a contingency plan.
		if (contingencyPlan == null)
		    contingencyPlan = plan;

		// If no valid plan was returned then the plan failed.
		if (contingencyPlan == null)
		    return null;

		// Get the next action from the child plan.
		return contingencyPlan.getNextAction(state);

	    }
	}
    }

    /**
     * Prepend an action to the plan and return itself.
     *
     * @param action
     *            the action to be prepended to this plan.
     * @return this plan with action prepended to it.
     */
    public Plan<S, A> prepend(A action) {
	actionSteps.add(0, action);
	return this;
    }

    /** Adds an if-state-then-plan statement at the end of the plan. */
    public void addIfStatement(S state, Plan<S, A> plan) {
	ifStatements.add(new IfStatement<>(state, plan));
    }

    /**
     * Returns a string representation of this plan.
     *
     * @return A string representation of this plan.
     */
    @Override
    public String toString() {
	StringBuilder s = new StringBuilder();
	s.append("[\n");
	int count = 0;
	for (A step : actionSteps) {
	    if (count++ > 0)
		s.append(",\n ");
	    s.append(step);
	}
	for (IfStatement<S, A> ifStatement : ifStatements) {
	    if (count++ > 0)
		s.append(",\n ");
	    s.append(ifStatement);
	}
	s.append("]");
	return s.toString();
    }

    /**
     * Represents an if-state-then-plan statement for use with AND-OR search;
     * explanation given on page 135 of AIMA3e.
     *
     * @author Ruediger Lunde
     */
    public static class IfStatement<S, A> {

	S state;
	Plan<S, A> plan;

	IfStatement(S state, Plan<S, A> plan) {
	    this.state = state;
	    this.plan = plan;
	}

	boolean testCondition(S state) {
	    return this.state.equals(state);
	}

	public Plan<S, A> getPlan() {
	    return plan;
	}

	public S getState() { return state; }

	/**
	 * Return string representation of this if-state-then-plan statement.
	 *
	 * @return A string representation of this if-state-then-plan statement.
	 */
	@Override
	public String toString() {
	    return "if " + state + " then " + plan;
	}
    }
}
