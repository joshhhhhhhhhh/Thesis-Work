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
    nd.non_deterministic_planning_action(Goals, prp);
    +start(system.time).

@action1[type(X, cell, temp), type(Y, cell, temp)] +!suck(X) : pos(X) & dirty(X) & not pos(Y) <-
    (not dirty(X)) & clean(X) & (not dirty(Y)) & clean(Y);
    (not dirty(X)) & clean(X).

@action2[type(X, cell, temp)] +!suck(X) : pos(X) & clean(X) <-
    dirty(X) & (not clean(X));
    None.

@action3[type(X, cell, temp)] +!right(X) : pos(X) & linked(X, c1) <-
    pos(c1) & not pos(X).

@action4[type(X, cell, temp)] +!left(X) : pos(X) & linked(c0, X) <-
    pos(c0) & not pos(X).

-!act : start(X) <-
    .print("TIME TAKEN: ", system.time - X).



