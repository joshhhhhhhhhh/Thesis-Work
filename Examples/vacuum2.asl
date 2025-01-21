
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
!des.


+!des : desires(Goals) <-
    .print("Goals to plan: ", Goals);
    nd.non_deterministic_planning_action(Goals, default);
    +start(system.time).


@action1[type(X, cell, temp)] +!suck(X) : pos(X) & dirty(X) & X == 49 <-
    (not dirty(X)) & clean(X) & (not dirty(X-1)) & clean(X-1);
    (not dirty(X)) & clean(X).

@action2[type(X, cell, temp)] +!suck(X) : pos(X) & dirty(X) & X == 0 <-
    (not dirty(X)) & clean(X) & (not dirty(X+1)) & clean(X+1);
    (not dirty(X)) & clean(X).

@action3[type(X, cell, temp)] +!suck(X) : pos(X) & dirty(X) & X \== 0 & X \== 49 <-
    (not dirty(X)) & clean(X) & (not dirty(X+1)) & clean(X+1) & (not dirty(X-1)) & clean(X-1);
    (not dirty(X)) & clean(X).

@action4[type(X, cell, temp)] +!suck(X) : pos(X) & clean(X) <-
    dirty(X) & (not clean(X));
    None.

@action5[type(X, cell, temp)] +!right(X) : pos(X) & X \== 49 <-
    pos(X+1) & not pos(X).

@action6[type(X, cell, temp)] +!left(X) : pos(X) & X \== 0 <-
    pos(X-1) & not pos(X).

-!act : start(X) <-
    .print("TIME TAKEN: ", system.time - X).

