package flee.dh.character

/**
 * Responsible for checking if a certain state transition is allowed
 */
fun interface StateTransitionRules {
    fun transitionAllowed(from: StateType, to: StateType): Boolean
}