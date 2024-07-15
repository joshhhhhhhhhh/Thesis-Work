package core.search.nondeterministic;

import core.search.framework.problem.ActionsFunction;
import core.search.framework.problem.GoalTest;
import jason.RevisionFailedException;

import java.util.List;

/**
 * Non-deterministic problems may have multiple results for a given state and
 * action; this class handles these results by mimicking Problem and replacing
 * ResultFunction (one result) with ResultsFunction (a set of results).
 *
 * @author Andrew Brown
 * @author Ruediger Lunde
 */
public class NondeterministicProblem<S, A> {

	protected S initialState;
	protected ActionsFunction<S, A> actionsFn;
	protected GoalTest<S> goalTest;
	protected ResultsFunction<S, A> resultsFn;

	/**
	 * Constructor
	 */
	public NondeterministicProblem(S initialState,
			ActionsFunction<S, A> actionsFn, ResultsFunction<S, A> resultsFn,
			GoalTest<S> goalTest) {
		this.initialState = initialState;
		this.actionsFn = actionsFn;
		this.resultsFn = resultsFn;
		this.goalTest = goalTest;
	}

	/**
	 * Returns the initial state of the agent.
	 *
	 * @return the initial state of the agent.
	 */
	public S getInitialState() {
		return initialState;
	}

	/**
	 * Returns <code>true</code> if the given state is a goal state.
	 *
	 * @return <code>true</code> if the given state is a goal state.
	 */
	public boolean testGoal(S state) {
		return goalTest.test(state);
	}

	/**
	 * Returns the description of the possible actions available to the agent.
	 */
	List<A> getActions(S state) {
		return actionsFn.apply(state);
	}

	/**
	 * Return the description of what each action does.
	 *
	 * @return the description of what each action does.
	 */
	public List<S> getResults(S state, A action) {
		try {
			return this.resultsFn.results(state, action);
		} catch (RevisionFailedException e) {
			e.printStackTrace();
			return null;
		}
	}

}
