


!des.
+!des : desires(Goals) <-
    .print("Goals to plan: ", Goals);
    nd.non_deterministic_planning_action(Goals, fondsat);
    +start(system.time).


@action1[type(B1, block, perm), type(B2, block, temp)] +!pickup(B1, B2) : emptyhand & on(B1, B2) & clear(B1) <-
    holding(B1) & holdingone & clear(B2) & (not emptyhand) & (not clear(B1)) & (not on(B1, B2));
    clear(B2) & (not on(B1, B2)) & ontable(B1).

@action2[type(B1, block, perm)] +!pickupfromtable(B1) : clear(B1) & ontable(B1) & emptyhand <-
    holdingone & (not ontable(B1)) & (not clear(B1)) & holding(B1) & (not(emptyhand));
    None.

@action3[type(B1, block, perm), type(B2, block, perm)] +!putonblock(B1, B2) : holdingone & holding(B1) & clear(B2) <-
    emptyhand & clear(B1) & (not holdingone) & (not holding(B1)) & on(B1, B2) & (not clear(B2));
    emptyhand & clear(B1) & (not holdingone) & (not holding(B1)) & ontable(B1).

@action4[type(B1, block, perm)] +!putdown(B1) : holdingone & holding(B1) <-
    ontable(B1) & emptyhand & clear(B1) & (not holdingone) & (not holding(B1)).

@action5[type(B1, block, temp), type(B2, block, perm), type(B3, block, temp)] +!picktower(B1, B2, B3) : emptyhand & on(B1, B2) & on(B2, B3) & clear(B1) <-
    holdingtwo & holding(B2) & clear(B3) & (not emptyhand) & (not clear(B1)) & (not on(B2, B3));
    None.

@action6[type(B1, block, temp), type(B2, block, perm), type(B3, block, perm)] +!puttoweronblock(B1, B2, B3) : holdingtwo & holding(B2) & clear(B3) & on(B1, B2) <-
    emptyhand & clear(B1) & (not holdingtwo) & (not holding(B2)) & on(B2, B3) & (not (clear(B3)));
    emptyhand & clear(B1) & (not holdingtwo) & (not holding(B2)) & ontable(B1) & ontable(B2) & clear(B2) & (not on(B1, B2)).

@action7[type(B1, block, temp), type(B2, block, perm)] +!puttowerdown(B1, B2) : holdingtwo & holding(B2) & on(B1, B2) <-
    emptyhand & clear(B1) & (not holdingtwo) & (not holding(B2)) & ontable(B2).

-!act : start(X) <-
    .print("TIME TO EXECUTE JASON PLANS: ", system.time - X).