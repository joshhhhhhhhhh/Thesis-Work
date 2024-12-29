

!des.
+!des : desires(Goals) <-
    .print("Goals to plan: ", Goals);
    nd.non_deterministic_planning_action(Goals, mynd);
    +start(system.time).

@action1[type(From, location, perm), type(To, location, perm)] +!movecar(From, To) : vehicleat(From) & road(From, To) & ~flattire <-
    vehicleat(To) & (not vehicleat(From)) & flattire & (not ~flattire);
    vehicleat(To) & (not vehicleat(From)).

@action2[type(Loc, location, temp)] +!loadtire(Loc) : vehicleat(Loc) & sparein(Loc) <-
    hasspare & (not sparein(Loc)) & (not ~hasspare).

@action3 +!changetire : hasspare <-
    (not hasspare) & ~hasspare & (not flattire) & ~flattire;
    None.

-!act : start(X) <-
    .print("TIME TAKEN: ", system.time - X).