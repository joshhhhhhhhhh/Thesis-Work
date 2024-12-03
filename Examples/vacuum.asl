//IN ENVIRONMENT SETUP
//
// object(cell, n) - for each cell  (0, 1, 2 ...)
// dirty(n) - for each cell (0, 1, 2 ...)
// pos(0)
// desires([clean(0), clean(1), ... , clean(n)])

//initial beliefs - Initialized in the environment

!des.
+!des : desires(Goals) <-
    .print("Goals to plan: ", Goals);
    nd.non_deterministic_planning_action(Goals, mynd);
    +start(system.time).

@action1[celltemp] +!suck(X) : pos(X) & dirty(X) & X == C4 <-
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

-!act : start(X) <-
    .print("TIME TAKEN: ", system.time - X).



