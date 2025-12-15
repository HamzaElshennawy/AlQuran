package com.hifnawy.alquran.shared.utils

import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.hifnawy.alquran.shared.utils.DurationExtensionFunctions.toFormattedTime
import com.hifnawy.alquran.shared.utils.ExoPlayerEx.PlayerState.Companion.asPlayerState
import kotlin.time.Duration.Companion.milliseconds

object ExoPlayerEx {

    /**
     * Represents the playback state of an [ExoPlayer] instance.
     *
     * This enum class maps the integer state constants from [Player]
     * to a more readable and type-safe format. It provides convenient properties
     * to check the current state.
     *
     * @property state The underlying integer state value from [Player].
     *
     * @author AbdElMoniem ElHifnawy
     *
     * @see Player
     */
    enum class PlayerState(val state: Int) {

        /**
         * Represents the state of the player when the media is buffering data before playback.
         *
         * @param state [Int] The integer constant from the [Player] representing the player state.
         *
         * @see Player.STATE_BUFFERING
         * @see isBuffering
         */
        BUFFERING(Player.STATE_BUFFERING),
        /**
         * Represents the state where the player is ready to `start`, `resume`, or `continue` playback.
         * The player has enough media buffered to `start` or `continue` playing.
         *
         * @param state [Int] The integer constant from the [Player] representing the player state.
         *
         * @see Player.STATE_READY
         * @see isReady
         */
        READY(Player.STATE_READY),
        /**
         * Represents the state of the player when playback has finished.
         *
         * @param state [Int] The integer constant from the [Player] representing the player state.
         *
         * @see Player.STATE_ENDED
         * @see isEnded
         */
        ENDED(Player.STATE_ENDED),
        /**
         * Represents the `idle` state of the player. This is the `initial` state, the state
         * when playback is `stopped`, and the state when a playback `error` occurs.
         *
         * @param state [Int] The integer constant from the [Player] representing the player state.
         *
         * @see Player.STATE_IDLE
         * @see isIdle
         */
        IDLE(Player.STATE_IDLE);

        /**
         * Indicates whether the player is currently buffering.
         *
         * @see BUFFERING
         */
        val isBuffering get() = this == BUFFERING

        /**
         * Indicates whether the player is currently ready.
         *
         * @see READY
         */
        val isReady get() = this == READY

        /**
         * Indicates whether the player is currently ended.
         *
         * @see ENDED
         */
        val isEnded get() = this == ENDED

        /**
         * Indicates whether the player is currently idle.
         *
         * @see IDLE
         */
        val isIdle get() = this == IDLE

        /**
         * Companion object for the [PlayerState] enum.
         *
         * @property asPlayerState [PlayerState] A utility property
         *   to convert an integer state from [Player] to its corresponding
         *   [PlayerState] enum constant.
         *
         * @author AbdElMoniem ElHifnawy
         *
         * @see PlayerState
         * @see Player
         */
        companion object {

            /**
             * Converts an integer state from [Player] to its corresponding [PlayerState].
             *
             * This extension property provides a safe and convenient way to translate the integer-based
             * state from Android's [Player] into a more readable and type-safe
             * [PlayerState] enum.
             *
             * @receiver [Int] The integer state value from [Player].
             *
             * @return [PlayerState] The matching [PlayerState] enum constant.
             *
             * @throws NoSuchElementException if no enum constant matches the provided state integer.
             */
            val Int.asPlayerState get() = entries.first { it.state == this }
        }
    }

    /**
     * Returns a detailed string representation of the [ExoPlayer]'s current state.
     * This is useful for logging and debugging purposes.
     *
     * The string includes:
     * - Playback state as [PlayerState].
     * - Loading status.
     * - Playing status.
     * - `Current` position, `buffered` position, and `total` duration in a `formatted time string`.
     *
     * **Example Usage:**
     * ```kotlin
     * println(exoPlayer.asString) // ExoPlayer(playbackState: READY, isLoading: false, isPlaying: true, durations: 00:15 (00:30) / 01:45)
     * ```
     */
    val ExoPlayer.asString
        get() = "ExoPlayer(" +
                "playbackState: ${playbackState.asPlayerState}, " +
                "isLoading: $isLoading, " +
                "isPlaying: $isPlaying, " +
                "durations: ${currentPosition.milliseconds.toFormattedTime()} " +
                "(${bufferedPosition.milliseconds.toFormattedTime()}) / " +
                duration.milliseconds.toFormattedTime() +
                ")"
}
