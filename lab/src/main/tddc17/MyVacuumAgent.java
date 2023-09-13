package tddc17;


import aima.core.environment.liuvacuum.*;
import aima.core.agent.Action;
import aima.core.agent.AgentProgram;
import aima.core.agent.Percept;
import aima.core.agent.impl.*;

import java.util.Random;
import java.util.Stack;

class MyAgentState
{
	public int[][] world = new int[30][30];
	public int initialized = 0;
	final int UNKNOWN 	= 0;
	final int WALL 		= 1;
	final int CLEAR 	= 2;
	final int DIRT		= 3;
	final int HOME		= 4;
	final int ACTION_NONE 			= 0;
	final int ACTION_MOVE_FORWARD 	= 1;
	final int ACTION_TURN_RIGHT 	= 2;
	final int ACTION_TURN_LEFT 		= 3;
	final int ACTION_SUCK	 		= 4;

	public int agent_x_position = 1;
	public int agent_y_position = 1;
	public int agent_last_action = ACTION_NONE;

	public static final int NORTH = 0;
	public static final int EAST = 1;
	public static final int SOUTH = 2;
	public static final int WEST = 3;
	public int agent_direction = EAST;

	MyAgentState()
	{
		for (int i=0; i < world.length; i++)
			for (int j=0; j < world[i].length ; j++)
				world[i][j] = UNKNOWN;
		world[1][1] = HOME;
		agent_last_action = ACTION_NONE;
	}
	// Based on the last action and the received percept updates the x & y agent position
	public void updatePosition(DynamicPercept p)
	{
		Boolean bump = (Boolean)p.getAttribute("bump");

		if (agent_last_action==ACTION_MOVE_FORWARD && !bump)
		{
			switch (agent_direction) {
				case MyAgentState.NORTH:
					agent_y_position--;
					break;
				case MyAgentState.EAST:
					agent_x_position++;
					break;
				case MyAgentState.SOUTH:
					agent_y_position++;
					break;
				case MyAgentState.WEST:
					agent_x_position--;
					break;
			}
		}

	}

	public void updateWorld(int x_position, int y_position, int info)
	{
		world[x_position][y_position] = info;
	}

	public void printWorldDebug()
	{
		for (int i=0; i < world.length; i++)
		{
			for (int j=0; j < world[i].length ; j++)
			{
				if (world[j][i]==UNKNOWN)
					System.out.print(" ? ");
				if (world[j][i]==WALL)
					System.out.print(" # ");
				if (world[j][i]==CLEAR)
					System.out.print(" . ");
				if (world[j][i]==DIRT)
					System.out.print(" D ");
				if (world[j][i]==HOME)
					System.out.print(" H ");
			}
			System.out.println("");
		}
	}
}

class MyAgentProgram implements AgentProgram {

	private int initnialRandomActions = 10;
	private Random random_generator = new Random();

	// Here you can define your variables!
	public int iterationCounter = 4000;
	public MyAgentState state = new MyAgentState();

	private Stack<Position> path = new Stack<>();

	private Stack<Position> homePath = new Stack<>();
	private boolean isHomeFounded = false;

	private int consecutiveTurnRight = 0;

	private boolean returnHomeMode = false;


	// moves the Agent to a random start position
	// uses percepts to update the Agent position - only the position, other percepts are ignored
	// returns a random action
	private Action moveToRandomStartPosition(DynamicPercept percept) {
		int action = random_generator.nextInt(6);
		initnialRandomActions--;
		state.updatePosition(percept);
		if(action==0) {
			state.agent_direction = ((state.agent_direction-1) % 4);
			if (state.agent_direction<0)
				state.agent_direction +=4;
			state.agent_last_action = state.ACTION_TURN_LEFT;
			return LIUVacuumEnvironment.ACTION_TURN_LEFT;
		} else if (action==1) {
			state.agent_direction = ((state.agent_direction+1) % 4);
			state.agent_last_action = state.ACTION_TURN_RIGHT;
			return LIUVacuumEnvironment.ACTION_TURN_RIGHT;
		}
		state.agent_last_action=state.ACTION_MOVE_FORWARD;
		return LIUVacuumEnvironment.ACTION_MOVE_FORWARD;
	}


	@Override
	public Action execute(Percept percept) {

		// DO NOT REMOVE this if condition!!!
		if (initnialRandomActions>0) {
			return moveToRandomStartPosition((DynamicPercept) percept);
		} else if (initnialRandomActions==0) {
			// process percept for the last step of the initial random actions
			initnialRandomActions--;
			state.updatePosition((DynamicPercept) percept);
			System.out.println("Processing percepts after the last execution of moveToRandomStartPosition()");
			state.agent_last_action=state.ACTION_SUCK;
			return LIUVacuumEnvironment.ACTION_SUCK;
		}

		// This example agent program will update the internal agent state while only moving forward.
		// START HERE - code below should be modified!

		System.out.println("x=" + state.agent_x_position);
		System.out.println("y=" + state.agent_y_position);
		System.out.println("dir=" + state.agent_direction);


		iterationCounter--;

		if (iterationCounter==0)
			return NoOpAction.NO_OP;

		DynamicPercept p = (DynamicPercept) percept;
		p.getAttribute("home");
		Boolean bump = (Boolean)p.getAttribute("bump");
		Boolean dirt = (Boolean)p.getAttribute("dirt");
		Boolean home = (Boolean)p.getAttribute("home");
		System.out.println("percept: " + p);

		// State update based on the percept value and the last action
		state.updatePosition((DynamicPercept)percept);
		if (bump) {
			// If the agent entered an obstacle then the current position should not have been added to the paths
			path.pop();
			if (isHomeFounded) homePath.pop();
			switch (state.agent_direction) {
				case MyAgentState.NORTH:
					state.updateWorld(state.agent_x_position,state.agent_y_position-1,state.WALL);
					break;
				case MyAgentState.EAST:
					state.updateWorld(state.agent_x_position+1,state.agent_y_position,state.WALL);
					break;
				case MyAgentState.SOUTH:
					state.updateWorld(state.agent_x_position,state.agent_y_position+1,state.WALL);
					break;
				case MyAgentState.WEST:
					state.updateWorld(state.agent_x_position-1,state.agent_y_position,state.WALL);
					break;
			}
		}
		if (dirt)
			state.updateWorld(state.agent_x_position,state.agent_y_position,state.DIRT);
		else
			state.updateWorld(state.agent_x_position,state.agent_y_position,state.CLEAR);

		state.printWorldDebug();


		// As soon as the home has been found, the variable isHomeFounded becomes true.
		// This will trigger the update of the path from the home each time the agent moves
		if((Boolean)p.getAttribute("home") && !isHomeFounded) isHomeFounded = true;

		// If the agent turns right 4 times consecutively this means that it has visited all the nodes.
		// This will trigger the return home mode
		if (state.agent_last_action == state.ACTION_TURN_RIGHT){
			if (consecutiveTurnRight++ == 3) returnHomeMode = true;
		} else {
			consecutiveTurnRight = 0;
		}


		// Next action selection based on the percept value
		if (dirt)
		{
			System.out.println("DIRT -> choosing SUCK action!");
			state.agent_last_action=state.ACTION_SUCK;
			return LIUVacuumEnvironment.ACTION_SUCK;
		}

		// When the agent turns left it is to visit the position which was on his left.
		// So the next action must be moving forward.
		else if (state.agent_last_action == state.ACTION_TURN_LEFT){
			path.push(getPosition());
			if (isHomeFounded) updateHomePath();
			return moveForward();
		}

		// This is the loop that replaces the main loop once the agent is in return home mode.
		// The agent want to go back up the path to the house.
		else if (returnHomeMode) {
			if((Boolean)p.getAttribute("home")){
				return NoOpAction.NO_OP;
			}
			else if (getForwardPosition().equals(homePath.peek())) {
				updateHomePath();
				return moveForward();
			}
			else {
				return turnRight();
			}
		}

		else
		{

			// It is the main loop which allows a breath first search to be performed
			// The paths are updated each time the agent moves forward.
			// If it advances towards a parent node, we remove this node from the paths.
			// Otherwise, we add the current position to the path.

			// If the position to the left of the agent is unknown, then he goes there.
			if(!isLeftKnown()) {
				return turnLeft();
			}

			// Otherwise, if the position in front of it is known and it is the position from which it came,
			// this means that it has visited all the child nodes and that it must go back to the parent node.
			// The agent therefore moves forward.
			else if ( !path.isEmpty() && path.peek().equals(getForwardPosition())) {
				if (isHomeFounded) updateHomePath();
				path.pop();
				return moveForward();
			}

			// Otherwise, if the forward position is known (whether it is an obstacle or not) the agent turns right.
			else if (isForwardKnown()) {
				return turnRight();
			}

			// Otherwise, the agent moves forward
			else
			{
				path.push(getPosition());
				if (isHomeFounded) updateHomePath();
				return moveForward();
			}
		}
	}

	/**
	 * This function updates the path to home.
	 * If it advances towards a parent node, we remove this node from the homePath.
	 * Otherwise, we add the current position to the homePath.
	 */
	private void updateHomePath(){
		if (!homePath.isEmpty() && homePath.peek().equals(getForwardPosition())){
			homePath.pop();
		} else {
			homePath.push(getPosition());
		}
	}

	/**
	 * This function encapsulates the actions necessary for the agent to turn right.
	 * Update direction
	 * Update agent last action
	 * @return action turn right
	 */
	private Action turnRight(){
		state.agent_direction = ((state.agent_direction+1) % 4);
		state.agent_last_action=state.ACTION_TURN_RIGHT;
		return LIUVacuumEnvironment.ACTION_TURN_RIGHT;
	}

	/**
	 * This function encapsulates the actions necessary for the agent to turn left.
	 * Update direction
	 * Update agent last action
	 * @return action turn left
	 */
	private Action turnLeft(){
		state.agent_direction = ((state.agent_direction-1) % 4);
		if (state.agent_direction<0)
			state.agent_direction +=4;
		state.agent_last_action=state.ACTION_TURN_LEFT;
		return LIUVacuumEnvironment.ACTION_TURN_LEFT;
	}

	/**
	 * This function encapsulates the actions necessary for the agent to move forward.
	 * Update agent last action
	 * @return action move forward
	 */
	private Action moveForward(){
		state.agent_last_action=state.ACTION_MOVE_FORWARD;
		return LIUVacuumEnvironment.ACTION_MOVE_FORWARD;
	}

	/**
	 * This function return the forward position of the agent using its current position
	 * @return forward position
	 */
	private Position getForwardPosition() {
		switch(state.agent_direction) {
			case MyAgentState.NORTH :
				return new Position(state.agent_x_position, state.agent_y_position - 1);

			case MyAgentState.EAST :
				return new Position(state.agent_x_position + 1, state.agent_y_position);

			case MyAgentState.SOUTH :
				return new Position(state.agent_x_position, state.agent_y_position + 1);

			case MyAgentState.WEST :
				return new Position(state.agent_x_position - 1, state.agent_y_position);

			default :
				throw new RuntimeException();
		}
	}

	/**
	 * This function return the left position of the agent using its current position
	 * @return left position
	 */
	private Position getLeftPosition() {
		switch(state.agent_direction) {
			case MyAgentState.NORTH :
				return new Position(state.agent_x_position - 1, state.agent_y_position);

			case MyAgentState.EAST :
				return new Position(state.agent_x_position , state.agent_y_position - 1);

			case MyAgentState.SOUTH :
				return new Position(state.agent_x_position + 1, state.agent_y_position);

			case MyAgentState.WEST :
				return new Position(state.agent_x_position , state.agent_y_position + 1);

			default :
				throw new RuntimeException();
		}
	}

	private Position getPosition(){
		return new Position(state.agent_x_position, state.agent_y_position);
	}


	/**
	 * We must take into account the fact that the position of the house is not marked as UNKNOWN even
	 * if the agent has not yet passed by it.
	 * @param position
	 * @return boolean : is the position known
	 */
	private boolean isPositionKnown(Position position) {
		final int positionState = state.world[position.getX()][position.getY()];
		return (positionState != state.UNKNOWN && positionState != state.HOME) || (positionState == state.HOME && isHomeFounded);
	}

	private boolean isForwardKnown () {
		return isPositionKnown(getForwardPosition());
	}

	private boolean isLeftKnown() {
		return isPositionKnown(getLeftPosition());
	}

	private class Position {
		private int x;
		private int y;

		public Position(int x, int y){
			this.x = x;
			this.y = y;
		}

		public int getX(){
			return x;
		}

		public int getY(){
			return y;
		}

		@Override
		public boolean equals(Object o){
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Position p = (Position) o;
			return (getX() == p.getX()) && (getY() == p.getY());
		}
	}

}

public class MyVacuumAgent extends AbstractAgent {
	public MyVacuumAgent() {
		super(new MyAgentProgram());
	}
}
