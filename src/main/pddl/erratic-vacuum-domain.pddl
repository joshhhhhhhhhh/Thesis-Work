(define (domain erratic-vacuum-domain)
    (:requirements :non-deterministic :equality :addition :subtraction :typing)
    (:types cell)
    (:predicates (pos ?x - cell) (clean ?x - cell) (dirty ?x - cell))
    (:action suck
        :parameters (?x - cell)
        :precondition (and (pos ?x) (clean ?x))
        :effect
            (oneof (and)
                   (and (dirty ?x) (not (clean ?x))))
    )
    (:action suck
        :parameters (?x - cell)
        :precondition (and (pos ?x) (dirty ?x) (= ?x 0))
        :effect
            (oneof (and (clean ?x) (not (dirty ?x)))
                   (and (clean ?x) (not (dirty ?x)) (clean (+ ?x 1)) (not (dirty (+ ?x 1)))))
    )
    (:action suck
        :parameters (?x - cell)
        :precondition (and (pos ?x) (dirty ?x) (= ?x 1))
        :effect
            (oneof (and (clean ?x) (not (dirty ?x)))
                   (and (clean ?x) (not (dirty ?x)) (clean (- ?x 1)) (not (dirty (- ?x 1)))))
    )
    (:action right
        :parameters (?x - cell)
        :precondition (not (pos 1))
        :effect (and (pos (+ ?x 1)) (not (pos ?x)))
    )
    (:action left
        :parameters (?x - cell)
        :precondition (not (pos 0))
        :effect (and (pos (- ?x 1)) (not (pos ?x)))
    )
)