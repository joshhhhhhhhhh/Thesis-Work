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

@action1[type(X, cell, temp)] +!suck(X) : pos(X) & dirty(X) & linked(c3, X) <-
    (not dirty(X)) & clean(X) & (not dirty(c3)) & clean(c3);
    (not dirty(X)) & clean(X).

@action2[type(X, cell, temp)] +!suck(X) : pos(X) & dirty(X) & linked(X, c1) <-
    (not dirty(X)) & clean(X) & (not dirty(c1)) & clean(c1);
    (not dirty(X)) & clean(X).

@action3[type(X, cell, temp), type(R, cell, temp), type(L, cell, temp)] +!suck(X, L, R) : pos(X) & dirty(X) & linked(L, X) & linked(X, R) <-
    (not dirty(X)) & clean(X) & (not dirty(R)) & clean(R) & (not dirty(L)) & clean(L);
    (not dirty(X)) & clean(X).

@action4[type(X, cell, temp)] +!suck(X) : pos(X) & clean(X) <-
    dirty(X) & (not clean(X));
    None.

@action5[type(X, cell, temp), type(R, cell, temp)] +!right(X, R) : pos(X) & linked(X, R) <-
    pos(R) & not pos(X).

@action6[type(X, cell, temp), type(L, cell, temp)] +!left(X, L) : pos(X) & linked(L, X) <-
    pos(L) & not pos(X).

-!act : start(X) <-
    .print("TIME TAKEN: ", system.time - X).



