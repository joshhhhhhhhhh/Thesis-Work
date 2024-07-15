(:action suck
    :parameters (?x, ?y)
    :precondition (pos(?x))
    :effect (and (when (clean ?x)
                       (or (and (dirty ?x) (not (clean ?x)))
                           (and (clean ?x) (not (dirty ?x)))))
                 (when (dirty ?x)
                       (and (?x != ?y)
                            (or (and (clean ?x) (not (dirty ?x)))
                                (and (clean ?x) (not (dirty ?x)) (clean ?y) (not (dirty ?y))))))
    )
)

(:action right
    :parameters ()
    :precondition (pos 0)
    :effect (and (pos 1) (not (pos 0)))
)

(:action left
    :parameters ()
    :precondition (pos 1)
    :effect (and (pos 0) (not (pos 1)))
)