
//object definitions
object(cell, 0).
object(cell, 1).
object(cell, 2).


//initial beliefs
//pos(0).
//dirty(0).
//dirty(1).
//dirty(2).

//des([clean(0), clean(1), clean(2)]).
!act.


+des(Goals) : true <-
    .print("Goals to plan: ", Goals);
    nd.non_deterministic_planning_action(Goals);
    +start(system.time).


@action1[celltemp] +!suck(X) : pos(X) & dirty(X) & X == 4 <-
    (not dirty(X)) & clean(X) & (not dirty(X-1)) & clean(X-1);
    (not dirty(X)) & clean(X).

@action2[celltemp] +!suck(X) : pos(X) & dirty(X) & X == 0 <-
    (not dirty(X)) & clean(X) & (not dirty(X+1)) & clean(X+1);
    (not dirty(X)) & clean(X).

@action3[celltemp] +!suck(X) : pos(X) & dirty(X) & X \== 0 & X \== 4 <-
    (not dirty(X)) & clean(X) & (not dirty(X+1)) & clean(X+1) & (not dirty(X-1)) & clean(X-1);
    (not dirty(X)) & clean(X).

@action4[celltemp] +!suck(X) : pos(X) & clean(X) <-
    dirty(X) & (not clean(X));
    None.

@action5[celltemp] +!right(X) : pos(X) & X \== 4 <-
    pos(X+1) & not pos(X).

@action6[celltemp] +!left(X) : pos(X) & X \== 0 <-
    pos(X-1) & not pos(X).

+!act : dirty(1) & pos(0) & dirty(0) & dirty(2) <-
    .print("A");
	suck;
	!act.

+!act : pos(0) & clean(0) & clean(1) & dirty(2) <-
    .print("B");
	suck;
	!act.

+!act : pos(0) & clean(1) & dirty(0) & dirty(2) <-
    .print("C");
	right;
	suck;
	!act.

+!act : clean(1) & dirty(0) & pos(1) & dirty(2) <-
    .print("D");
	suck;
	!act.

+!act : dirty(1) & dirty(0) & pos(1) & dirty(2) <-
    .print("E");
	suck;
	!act.

+!act : dirty(1) & pos(0) & clean(0) & dirty(2) <-
    .print("F");
	right;
	suck;
	!act.

+!act : clean(0) & clean(1) & pos(1) & dirty(2) <-
    .print("G");
	right;
	suck;
	!act.

-!act : start(X) <-
    .print("TIME TAKEN: ", system.time - X).

