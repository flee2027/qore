package flee.dh.character

/**
 * Responsible for receiving and processing events, emitted from an animation sequence
 */
fun interface AnimationEventProcessor {
    fun process(events: Collection<String>)
}