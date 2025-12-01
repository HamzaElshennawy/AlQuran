
/*
 * An exceptionally complex and intricate composable that establishes the core navigation architecture
 * of the application using Jetpack Navigation Compose. This file is the central nervous system
 * for screen transitions, state management, and highly sophisticated, context-aware animations.
 *
 * **WARNING: Extreme Complexity Ahead**
 * This file is not for the faint of heart. It orchestrates a delicate and convoluted ballet of
 * animations and navigation logic. The transitions are not simple fades or slides; they are
 * meticulously crafted, state-dependent, and direction-aware. Modifying any part of this
 * component, from the transition definitions to the NavHost setup, requires a profound
 * understanding of the entire navigation flow and its potential side effects. Proceed with
 * extreme caution, and think twice before making any changes. You will most likely regret
 * hasty modifications.
 *
 * ---
 *
 * ### Core Responsibilities:
 *
 * 1.  **Navigation Graph Definition**: It defines the complete navigation graph using a [NavHost],
 *     mapping string-based routes to their corresponding composable screen destinations.
 *
 * 2.  **Stateful, Context-Aware Animations**: This is the heart of the complexity. The function
 *     implements a highly sophisticated animation system where transitions between screens are
 *     not static. Instead, they dynamically change based on:
 *     -   **Navigation Direction**: The animation for navigating from Screen A to Screen B is
 *         different from navigating from B to A (e.g., vertical slide for master-detail,
 *         horizontal for sibling screens).
 *     -   **Navigation Action**: Enter, exit, pop-enter, and pop-exit transitions are all uniquely
 *         defined for each screen, ensuring a cohesive and intuitive user experience. For example,
 *         a "pop" transition is a visually distinct reversal of the initial "enter" transition.
 *     -   **Screen Hierarchy**: It establishes a clear visual hierarchy. For instance, navigating
 *
 * THINK TWICE AND CAREFULLY BEFORE MODIFYING THIS FILE OR ADDING ANYTHING TO IT
 * YOU'LL MOST PROBABLY REGRET IT 🤣🤣🤣
 */


package com.hifnawy.alquran.view.navigation

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Transition.Segment
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.gson.Gson
import com.hifnawy.alquran.MobileApplication
import com.hifnawy.alquran.shared.model.Moshaf
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.utils.ModifierEx.onTouch
import com.hifnawy.alquran.utils.ModifierEx.safeImePadding
import com.hifnawy.alquran.utils.sampleReciters
import com.hifnawy.alquran.utils.sampleSurahs
import com.hifnawy.alquran.view.focusManager
import com.hifnawy.alquran.view.player.PlayerContainer
import com.hifnawy.alquran.view.screens.FavoritesScreen
import com.hifnawy.alquran.view.screens.RecitersScreen
import com.hifnawy.alquran.view.screens.Screen
import com.hifnawy.alquran.view.screens.SettingsScreen
import com.hifnawy.alquran.view.screens.SurahsScreen
import com.hifnawy.alquran.viewModel.MediaViewModel

/**
 * Type alias for [AnimatedContentTransitionScope] with a [NavBackStackEntry] as the transition target.
 *
 * This type alias is used to shorten the type of the scope parameter in the [EnterTransition] and [ExitTransition]
 * extension properties as well as the [fadingSlideInTransition] and [fadingSlideOutTransition] extension functions.
 * It is used to make the code more readable and reduce the length of the lines.
 *
 * Using this type alias allows us to write shorter and more concise code.
 * For example, instead of writing:
 *
 * @author AbdElMoniem ElHifnawy
 *
 * @see AnimatedContentTransitionScope
 * @see NavBackStackEntry
 * @see EnterTransition
 * @see ExitTransition
 * @see fadingSlideInTransition
 * @see fadingSlideOutTransition
 */
private typealias TransitionScope = AnimatedContentTransitionScope<NavBackStackEntry>

/**
 * The duration in milliseconds for the enter transition animations.
 *
 * This value is used to define how long the enter animations, such as fade-in and slide-in,
 * will take to complete when a new screen is displayed.
 *
 * @see enterFadeAnimationSpec
 * @see slideAnimationSpec
 */
private const val ENTER_ANIMATION_DURATION = 500

/**
 * The duration in milliseconds for exit animations when navigating between screens.
 * This value is used for both fade-out and slide-out animations.
 *
 * @see exitFadeAnimationSpec
 * @see recitersExitTransition
 * @see surahsExitTransition
 * @see favoritesExitTransition
 * @see settingsExitTransition
 */
private const val EXIT_ANIMATION_DURATION = 700

/**
 * A factor that determines the starting offset for an enter transition.
 *
 * When a new screen enters, it slides in from an initial offset. This factor is multiplied by the
 * full width or height of the screen to calculate this offset. A value of `1f` means the screen
 * starts completely off-screen (at 100% of the screen's dimension) and slides into view.
 *
 * @see fadingSlideInTransition
 * @see EXIT_OFFSET_FACTOR
 * @see POP_ENTER_OFFSET_FACTOR
 */
private const val ENTER_OFFSET_FACTOR = 1f

/**
 * A factor used to determine the target offset for exit transitions.
 *
 * This value scales the full slide distance (either width or height) to create a parallax effect,
 * where the exiting screen moves slower than the entering screen. A value of `0.5f` means the
 * exiting screen will slide to half of the container's dimension.
 *
 * This helps create a sense of depth and hierarchy in the navigation animations.
 *
 * @see fadingSlideOutTransition
 * @see ENTER_OFFSET_FACTOR
 */
private const val EXIT_OFFSET_FACTOR = 0.5f

/**
 * A factor used to calculate the initial offset for the "pop enter" slide animation.
 *
 * When a screen is brought back onto the stack (e.g., by pressing the back button), this factor determines
 * how far `off-screen` it starts before sliding into its final position. A smaller value results in a
 * more subtle slide-in effect, as the screen starts closer to its destination.
 *
 * This value is multiplied by the full width or height of the container to get the final pixel offset.
 * For example, a value of `0.2f` means the screen will start its animation at 20% of the container's
 * dimension away from its final position.
 *
 * @see recitersPopEnterTransition
 * @see surahsPopEnterTransition
 * @see favoritesPopEnterTransition
 * @see settingsPopEnterTransition
 * @see ENTER_OFFSET_FACTOR
 */
private const val POP_ENTER_OFFSET_FACTOR = 0.2f

/**
 * A factor that determines the target offset for a pop exit transition.
 *
 * This value is used to calculate the final position of the exiting screen when the user navigates back (pops the back stack).
 * A value of `1f` means the screen will slide out completely by its full width or height, effectively moving it entirely out of view.
 *
 * @see recitersPopExitTransition
 * @see surahsPopExitTransition
 * @see favoritesPopExitTransition
 * @see settingsPopExitTransition
 */
private const val POP_EXIT_OFFSET_FACTOR = 1f

/**
 * Animation specification for the fade-in effect during an enter transition.
 *
 * This [tween] animation controls the alpha property of a composable as it enters the screen,
 * creating a smooth fade-in effect. The duration is set by [ENTER_ANIMATION_DURATION].
 * It is used in conjunction with slide animations to create a combined `fading slide` transition.
 *
 * @see tween
 * @see fadingSlideInTransition
 * @see ENTER_ANIMATION_DURATION
 */
private val enterFadeAnimationSpec = tween<Float>(durationMillis = ENTER_ANIMATION_DURATION)

/**
 * Animation specification for the fade-out effect during an exit transition.
 *
 * This [tween] animation controls the alpha component of a composable as it fades out of the screen.
 * It is configured with a duration of [EXIT_ANIMATION_DURATION].
 *
 * @see tween
 * @see EXIT_ANIMATION_DURATION
 * @see fadingSlideOutTransition
 */
private val exitFadeAnimationSpec = tween<Float>(durationMillis = EXIT_ANIMATION_DURATION)

/**
 * Animation specification for the slide-in/slide-out transitions.
 *
 * This [tween] animation is used to define the movement of screens as they slide into or out of view.
 * The duration is set to [ENTER_ANIMATION_DURATION] to ensure a consistent feel with the fade animations.
 * It animates the `IntOffset` of a composable, which controls its position on the screen.
 *
 * @see tween
 * @see IntOffset
 * @see ENTER_ANIMATION_DURATION
 * @see fadingSlideInTransition
 * @see fadingSlideOutTransition
 */
private val slideAnimationSpec = tween<IntOffset>(durationMillis = ENTER_ANIMATION_DURATION)

/**
 * Calculates an offset by multiplying this integer by a given float factor.
 *
 * This infix extension function provides a concise way to scale an integer value (typically
 * a width or height in pixels) by a floating-point factor. The result is then converted
 * back to an integer, effectively truncating any fractional part. It is primarily used in this
 * file to calculate the initial or target offsets for slide animations in navigation transitions.
 *
 * For example,
 * ```
 * fullWidth offsetBy 0.5f
 * ```
 * will return half of the `fullWidth`.
 *
 * @param factor [Float] The floating-point number to multiply this integer by.
 *
 * @return [Int] The result of the multiplication, converted to an [Int].
 */
private infix fun Int.offsetBy(factor: Float) = (this * factor).toInt()

/**
 * Defines the enter transition for the [RecitersScreen].
 *
 * This property dynamically determines the slide direction based on the previous screen ([initialState][Segment.initialState]).
 *
 * - If navigating from the [SurahsScreen], the [RecitersScreen] will slide in from the top ([SlideDirection.Down]).
 *   This creates a vertical transition effect between the master ([RecitersScreen]) and detail ([SurahsScreen]) views.
 * - For all other cases, it defaults to a standard horizontal slide-in from the end ([SlideDirection.End]).
 *
 * The transition combines a fade-in effect with the slide motion.
 *
 * @return [EnterTransition] The calculated enter transition.
 *
 * @see fadingSlideInTransition
 * @see ENTER_ANIMATION_DURATION
 * @see ENTER_OFFSET_FACTOR
 * @see Screen.Surahs
 */
private val TransitionScope.recitersEnterTransition
    get() = when {
        initialState.destination.route?.startsWith(Screen.Surahs.route) == true -> fadingSlideInTransition(
                towards = SlideDirection.Down,
                fadeAnimationSpec = enterFadeAnimationSpec,
                slideAnimationSpec = slideAnimationSpec,
                initialOffset = { fullHeight -> fullHeight offsetBy ENTER_OFFSET_FACTOR }
        )

        else                                                                    -> fadingSlideInTransition(
                towards = SlideDirection.End,
                fadeAnimationSpec = enterFadeAnimationSpec,
                slideAnimationSpec = slideAnimationSpec,
                initialOffset = { fullWidth -> fullWidth offsetBy ENTER_OFFSET_FACTOR }
        )
    }

/**
 * Defines the exit transition for the [RecitersScreen].
 *
 * This transition is context-aware and changes based on the destination screen ([targetState][Segment.targetState]).
 *
 * - When navigating to the [SurahsScreen], the [RecitersScreen] slides out towards the top ([SlideDirection.Up]).
 *   This creates a master-detail effect where the master list ([RecitersScreen]) moves up to make way for the detail view ([SurahsScreen]).
 * - For all other navigation targets, it performs a standard horizontal slide out towards the start ([SlideDirection.Start]).
 *
 * Both transitions are combined with a fade-out effect.
 *
 * @return [ExitTransition] The appropriate exit transition for the current navigation action.
 *
 * @see fadingSlideOutTransition
 * @see EXIT_ANIMATION_DURATION
 * @see EXIT_OFFSET_FACTOR
 * @see Screen.Surahs
 */
private val TransitionScope.recitersExitTransition
    get() = when {
        targetState.destination.route?.startsWith(Screen.Surahs.route) == true -> fadingSlideOutTransition(
                towards = SlideDirection.Up,
                fadeAnimationSpec = exitFadeAnimationSpec,
                slideAnimationSpec = slideAnimationSpec,
                targetOffset = { fullHeight -> fullHeight offsetBy EXIT_OFFSET_FACTOR }
        )

        else                                                                   -> fadingSlideOutTransition(
                towards = SlideDirection.Start,
                fadeAnimationSpec = exitFadeAnimationSpec,
                slideAnimationSpec = slideAnimationSpec,
                targetOffset = { fullWidth -> fullWidth offsetBy EXIT_OFFSET_FACTOR }
        )
    }

/**
 * Defines the pop enter transition for the [RecitersScreen].
 *
 * This transition is triggered when the user navigates back to the [RecitersScreen] from another screen.
 * It uses a subtle slide-in effect, controlled by [POP_ENTER_OFFSET_FACTOR], which makes the screen
 * appear to slide in from a closer starting point compared to the standard enter transition.
 *
 * The slide direction is determined by the screen being popped from the back stack:
 * - If popping from the [SurahsScreen], it slides in from the top ([SlideDirection.Down]).
 * - In all other cases, it slides in from the end ([SlideDirection.End]).
 *
 * This creates a consistent and hierarchical navigation feel, especially in the master-detail flow
 * between the reciters and surahs lists.
 *
 * @return [EnterTransition] The calculated pop enter transition.
 *
 * @see fadingSlideInTransition
 * @see POP_ENTER_OFFSET_FACTOR
 * @see Screen.Surahs
 */
private val TransitionScope.recitersPopEnterTransition
    get() = when {
        initialState.destination.route?.startsWith(Screen.Surahs.route) == true -> fadingSlideInTransition(
                towards = SlideDirection.Down,
                fadeAnimationSpec = enterFadeAnimationSpec,
                slideAnimationSpec = slideAnimationSpec,
                initialOffset = { fullHeight -> fullHeight offsetBy POP_ENTER_OFFSET_FACTOR }
        )

        else                                                                    -> fadingSlideInTransition(
                towards = SlideDirection.End,
                fadeAnimationSpec = enterFadeAnimationSpec,
                slideAnimationSpec = slideAnimationSpec,
                initialOffset = { fullWidth -> fullWidth offsetBy POP_ENTER_OFFSET_FACTOR }
        )
    }

/**
 * Defines the pop exit transition for the [RecitersScreen].
 *
 * This transition is triggered when the user navigates back from the [RecitersScreen], causing it to be
 * removed from the back stack. The animation behavior is conditional on the destination screen:
 *
 * - If navigating back to the [SurahsScreen], the [RecitersScreen] will slide up and out of view
 *   ([SlideDirection.Up]). This complements the vertical pop enter transition of the [SurahsScreen],
 *   maintaining a consistent master-detail feel.
 * - In all other cases (e.g., exiting the app from the [RecitersScreen]), it performs a standard
 *   horizontal slide-out to the start ([SlideDirection.Start]).
 *
 * In both scenarios, the screen slides out completely ([POP_EXIT_OFFSET_FACTOR]) and fades out simultaneously.
 *
 * @return [ExitTransition] The configured exit transition.
 *
 * @see fadingSlideOutTransition
 * @see POP_EXIT_OFFSET_FACTOR
 * @see Screen.Surahs
 */
private val TransitionScope.recitersPopExitTransition
    get() = when {
        targetState.destination.route?.startsWith(Screen.Surahs.route) == true -> fadingSlideOutTransition(
                towards = SlideDirection.Up,
                fadeAnimationSpec = exitFadeAnimationSpec,
                slideAnimationSpec = slideAnimationSpec,
                targetOffset = { fullHeight -> fullHeight offsetBy POP_EXIT_OFFSET_FACTOR }
        )

        else                                                                   -> fadingSlideOutTransition(
                towards = SlideDirection.Start,
                fadeAnimationSpec = exitFadeAnimationSpec,
                slideAnimationSpec = slideAnimationSpec,
                targetOffset = { fullWidth -> fullWidth offsetBy POP_EXIT_OFFSET_FACTOR }
        )
    }

/**
 * Defines the enter transition for the [SurahsScreen].
 *
 * This property determines how the [SurahsScreen] animates into view based on the screen it is
 * being navigated from ([initialState][Segment.initialState]).
 *
 * - If navigating from the [RecitersScreen], the [SurahsScreen] will slide in from the bottom, moving
 *   upwards ([SlideDirection.Up]). This creates a vertical transition, reinforcing the master-detail
 *   relationship between the two screens.
 * - In all other navigation scenarios, it defaults to a standard horizontal slide-in from the end
 *   of the screen ([SlideDirection.End]).
 *
 * Both transitions are combined with a fade-in effect for a smoother visual experience.
 *
 * @return [EnterTransition] The calculated enter transition for the [SurahsScreen].
 *
 * @see fadingSlideInTransition
 * @see ENTER_ANIMATION_DURATION
 * @see ENTER_OFFSET_FACTOR
 * @see Screen.Reciters
 */
private val TransitionScope.surahsEnterTransition
    get() = when (initialState.destination.route) {
        Screen.Reciters.route -> fadingSlideInTransition(
                towards = SlideDirection.Up,
                fadeAnimationSpec = enterFadeAnimationSpec,
                slideAnimationSpec = slideAnimationSpec,
                initialOffset = { fullHeight -> fullHeight offsetBy ENTER_OFFSET_FACTOR }
        )

        else                  -> fadingSlideInTransition(
                towards = SlideDirection.End,
                fadeAnimationSpec = enterFadeAnimationSpec,
                slideAnimationSpec = slideAnimationSpec,
                initialOffset = { fullWidth -> fullWidth offsetBy ENTER_OFFSET_FACTOR }
        )
    }

/**
 * Defines the exit transition for the [SurahsScreen].
 *
 * This property determines how the [SurahsScreen] animates out of view based on the destination screen
 * ([targetState][androidx.compose.animation.core.Transition.Segment.targetState]).
 *
 * - When navigating back to the [RecitersScreen], the [SurahsScreen] will slide down ([SlideDirection.Down]).
 *   This creates a cohesive vertical transition that mirrors the slide up enter animation, reinforcing the
 *   master-detail relationship between the two screens.
 * - In all other navigation scenarios, it performs a standard horizontal slide out to the start ([SlideDirection.Start]).
 *
 * The transition combines a fade-out effect with the slide motion for a smooth visual experience.
 *
 * @return [ExitTransition] The calculated exit transition.
 *
 * @see fadingSlideOutTransition
 * @see EXIT_ANIMATION_DURATION
 * @see EXIT_OFFSET_FACTOR
 * @see Screen.Reciters
 */
private val TransitionScope.surahsExitTransition
    get() = when (targetState.destination.route) {
        Screen.Reciters.route -> fadingSlideOutTransition(
                towards = SlideDirection.Down,
                fadeAnimationSpec = exitFadeAnimationSpec,
                slideAnimationSpec = slideAnimationSpec,
                targetOffset = { fullHeight -> fullHeight offsetBy EXIT_OFFSET_FACTOR }
        )

        else                  -> fadingSlideOutTransition(
                towards = SlideDirection.Start,
                fadeAnimationSpec = exitFadeAnimationSpec,
                slideAnimationSpec = slideAnimationSpec,
                targetOffset = { fullWidth -> fullWidth offsetBy EXIT_OFFSET_FACTOR }
        )
    }

/**
 * Defines the pop enter transition for the [SurahsScreen]. This animation is used when the user navigates
 * back to the [SurahsScreen] (e.g., from a screen that was on top of it).
 *
 * The transition behavior is conditional:
 * - If the previous screen was the [RecitersScreen], the [SurahsScreen] will slide in from the top
 *   ([SlideDirection.Up]). This maintains the vertical slide relationship established during the forward navigation.
 * - In all other cases, it performs a standard horizontal slide-in from the end ([SlideDirection.End]).
 *
 * A fade-in effect is combined with the slide. The initial offset is determined by [POP_ENTER_OFFSET_FACTOR]
 * to create a subtle slide-in effect, making it appear as if the screen is returning from a shallow depth.
 *
 * @return [EnterTransition] The calculated pop enter transition.
 *
 * @see fadingSlideInTransition
 * @see POP_ENTER_OFFSET_FACTOR
 * @see Screen.Reciters
 */
private val TransitionScope.surahsPopEnterTransition
    get() = when (initialState.destination.route) {
        Screen.Reciters.route -> fadingSlideInTransition(
                towards = SlideDirection.Up,
                fadeAnimationSpec = enterFadeAnimationSpec,
                slideAnimationSpec = slideAnimationSpec,
                initialOffset = { fullHeight -> fullHeight offsetBy POP_ENTER_OFFSET_FACTOR }
        )

        else                  -> fadingSlideInTransition(
                towards = SlideDirection.End,
                fadeAnimationSpec = enterFadeAnimationSpec,
                slideAnimationSpec = slideAnimationSpec,
                initialOffset = { fullWidth -> fullWidth offsetBy POP_ENTER_OFFSET_FACTOR }
        )
    }

/**
 * Defines the pop exit transition for the [SurahsScreen].
 *
 * This transition is triggered when the user navigates back from the [SurahsScreen].
 * The animation direction depends on the screen being navigated to ([targetState][androidx.compose.animation.core.Transition.Segment.targetState]).
 *
 * - If navigating back to the [RecitersScreen], the [SurahsScreen] will slide down and out of view ([SlideDirection.Down]).
 *   This reverses the enter animation, creating a cohesive vertical transition between the master ([RecitersScreen]) and detail ([SurahsScreen]) views.
 * - In all other backward navigation scenarios, it performs a standard horizontal slide out towards the start ([SlideDirection.Start]).
 *
 * The screen slides completely out of view, as determined by the [POP_EXIT_OFFSET_FACTOR].
 *
 * @return [ExitTransition] The calculated pop exit transition.
 *
 * @see fadingSlideOutTransition
 * @see POP_EXIT_OFFSET_FACTOR
 * @see Screen.Reciters
 */
private val TransitionScope.surahsPopExitTransition
    get() = when (targetState.destination.route) {
        Screen.Reciters.route -> fadingSlideOutTransition(
                towards = SlideDirection.Down,
                fadeAnimationSpec = exitFadeAnimationSpec,
                slideAnimationSpec = slideAnimationSpec,
                targetOffset = { fullHeight -> fullHeight offsetBy POP_EXIT_OFFSET_FACTOR }
        )

        else                  -> fadingSlideOutTransition(
                towards = SlideDirection.Start,
                fadeAnimationSpec = exitFadeAnimationSpec,
                slideAnimationSpec = slideAnimationSpec,
                targetOffset = { fullWidth -> fullWidth offsetBy POP_EXIT_OFFSET_FACTOR }
        )
    }

/**
 * Defines the enter transition for the [FavoritesScreen].
 *
 * This transition's direction depends on the screen the user is navigating from:
 * - When coming from the [SettingsScreen], the [FavoritesScreen] slides in from the right ([SlideDirection.End]).
 * - In all other cases (e.g., from the [RecitersScreen] or [SurahsScreen]), it slides in from the left ([SlideDirection.Start]).
 *
 * This directional logic helps maintain a clear spatial relationship between the screens in the bottom navigation bar,
 * where `Favorites` is positioned to the left of `Settings`.
 *
 * The animation combines a fade-in with the slide motion.
 *
 * @return [EnterTransition] The calculated enter transition.
 *
 * @see fadingSlideInTransition
 * @see Screen.Settings
 * @see Screen.Favorites
 */
private val TransitionScope.favoritesEnterTransition
    get() = when (initialState.destination.route) {
        Screen.Settings.route -> fadingSlideInTransition(
                towards = SlideDirection.End,
                fadeAnimationSpec = enterFadeAnimationSpec,
                slideAnimationSpec = slideAnimationSpec,
                initialOffset = { fullWidth -> fullWidth offsetBy ENTER_OFFSET_FACTOR }
        )

        else                  -> fadingSlideInTransition(
                towards = SlideDirection.Start,
                fadeAnimationSpec = enterFadeAnimationSpec,
                slideAnimationSpec = slideAnimationSpec,
                initialOffset = { fullWidth -> fullWidth offsetBy ENTER_OFFSET_FACTOR }
        )
    }

/**
 * Defines the exit transition for the [FavoritesScreen].
 *
 * This property determines the slide direction when navigating away from the [FavoritesScreen].
 * The animation is a combination of a fade-out and a slide.
 *
 * - If navigating to the [SettingsScreen], the [FavoritesScreen] will slide out towards the start
 *   ([SlideDirection.Start]), creating a horizontal transition.
 * - For all other navigation targets, it will slide out towards the end ([SlideDirection.End]).
 *
 * @return [ExitTransition] The configured exit transition.
 *
 * @see fadingSlideOutTransition
 * @see EXIT_ANIMATION_DURATION
 * @see EXIT_OFFSET_FACTOR
 * @see Screen.Settings
 */
private val TransitionScope.favoritesExitTransition
    get() = when (targetState.destination.route) {
        Screen.Settings.route -> fadingSlideOutTransition(
                towards = SlideDirection.Start,
                fadeAnimationSpec = exitFadeAnimationSpec,
                slideAnimationSpec = slideAnimationSpec,
                targetOffset = { fullWidth -> fullWidth offsetBy EXIT_OFFSET_FACTOR }
        )

        else                  -> fadingSlideOutTransition(
                towards = SlideDirection.End,
                fadeAnimationSpec = exitFadeAnimationSpec,
                slideAnimationSpec = slideAnimationSpec,
                targetOffset = { fullWidth -> fullWidth offsetBy EXIT_OFFSET_FACTOR }
        )
    }

/**
 * Defines the pop enter transition for the [FavoritesScreen].
 *
 * This transition is triggered when the user navigates back to the [FavoritesScreen] (e.g., by
 * pressing the back button). The animation direction depends on the screen being popped from the
 * back stack.
 *
 * - If returning from the [SettingsScreen], the [FavoritesScreen] will slide in from the end ([SlideDirection.End]).
 * - In all other cases, it will slide in from the start ([SlideDirection.Start]).
 *
 * The slide animation starts from a reduced offset, determined by [POP_ENTER_OFFSET_FACTOR],
 * creating a subtle re-entry effect.
 *
 * @return [EnterTransition] The calculated "pop enter" transition.
 *
 * @see fadingSlideInTransition
 * @see POP_ENTER_OFFSET_FACTOR
 * @see Screen.Settings
 */
private val TransitionScope.favoritesPopEnterTransition
    get() = when (initialState.destination.route) {
        Screen.Settings.route -> fadingSlideInTransition(
                towards = SlideDirection.End,
                fadeAnimationSpec = enterFadeAnimationSpec,
                slideAnimationSpec = slideAnimationSpec,
                initialOffset = { fullWidth -> fullWidth offsetBy POP_ENTER_OFFSET_FACTOR }
        )

        else                  -> fadingSlideInTransition(
                towards = SlideDirection.Start,
                fadeAnimationSpec = enterFadeAnimationSpec,
                slideAnimationSpec = slideAnimationSpec,
                initialOffset = { fullWidth -> fullWidth offsetBy POP_ENTER_OFFSET_FACTOR }
        )
    }

/**
 * Defines the pop exit transition for the [FavoritesScreen] when the user navigates back.
 *
 * This transition is triggered when the [FavoritesScreen] is removed from the back stack. The direction of the
 * slide-out animation depends on the destination screen ([targetState][androidx.compose.animation.core.Transition.Segment.targetState]).
 *
 * - If navigating back to the [SettingsScreen], the [FavoritesScreen] will slide out towards the start ([SlideDirection.Start]).
 * - In all other cases (e.g., navigating back to the main screens), it will slide out towards the end ([SlideDirection.End]).
 *
 * This animation slides the screen completely out of view, using a slide offset factor of `1.0` ([POP_EXIT_OFFSET_FACTOR]),
 * combined with a fade-out effect.
 *
 * @return [ExitTransition] The configured exit transition for the pop action.
 *
 * @see fadingSlideOutTransition
 * @see EXIT_ANIMATION_DURATION
 * @see POP_EXIT_OFFSET_FACTOR
 * @see Screen.Settings
 */
private val TransitionScope.favoritesPopExitTransition
    get() = when (targetState.destination.route) {
        Screen.Settings.route -> fadingSlideOutTransition(
                towards = SlideDirection.Start,
                fadeAnimationSpec = exitFadeAnimationSpec,
                slideAnimationSpec = slideAnimationSpec,
                targetOffset = { fullWidth -> fullWidth offsetBy POP_EXIT_OFFSET_FACTOR }
        )

        else                  -> fadingSlideOutTransition(
                towards = SlideDirection.End,
                fadeAnimationSpec = exitFadeAnimationSpec,
                slideAnimationSpec = slideAnimationSpec,
                targetOffset = { fullWidth -> fullWidth offsetBy POP_EXIT_OFFSET_FACTOR }
        )
    }

/**
 * Defines the enter transition for the [SettingsScreen].
 *
 * This transition always slides the screen in from the start (left) of the container,
 * accompanied by a fade-in effect. This creates a consistent and predictable
 * animation when navigating to the settings.
 *
 * @return [EnterTransition] The configured enter transition, which combines a fade-in
 * and a slide-in from the start.
 *
 * @see fadingSlideInTransition
 * @see ENTER_ANIMATION_DURATION
 * @see ENTER_OFFSET_FACTOR
 * @see Screen.Settings
 */
private val TransitionScope.settingsEnterTransition
    get() = fadingSlideInTransition(
            towards = SlideDirection.Start,
            fadeAnimationSpec = enterFadeAnimationSpec,
            slideAnimationSpec = slideAnimationSpec,
            initialOffset = { fullWidth -> fullWidth offsetBy ENTER_OFFSET_FACTOR }
    )

/**
 * Defines the exit transition for the [SettingsScreen].
 *
 * When navigating away from the [SettingsScreen], it performs a fading slide-out transition
 * towards the end of the screen ([SlideDirection.End]).
 *
 * The transition combines a fade-out effect with a horizontal slide, creating a smooth and
 * layered animation. The slide distance is scaled by [EXIT_OFFSET_FACTOR] to create a
 * parallax effect where the exiting screen moves slower than the entering one.
 *
 * @return [ExitTransition] The configured exit transition.
 *
 * @see fadingSlideOutTransition
 * @see EXIT_OFFSET_FACTOR
 * @see EXIT_ANIMATION_DURATION
 * @see Screen.Settings
 */
private val TransitionScope.settingsExitTransition
    get() = fadingSlideOutTransition(
            towards = SlideDirection.End,
            fadeAnimationSpec = exitFadeAnimationSpec,
            slideAnimationSpec = slideAnimationSpec,
            targetOffset = { fullWidth -> fullWidth offsetBy EXIT_OFFSET_FACTOR }
    )

/**
 * Defines the pop enter transition for the [SettingsScreen].
 *
 * This transition is triggered when the user navigates back to the [SettingsScreen] from another screen.
 * It creates a slide-in effect from the start of the screen, combined with a fade-in.
 * The slide offset is determined by [POP_ENTER_OFFSET_FACTOR], resulting in a more subtle re-entry animation
 * compared to the initial enter transition.
 *
 * @return [EnterTransition] The calculated pop enter transition.
 *
 * @see fadingSlideInTransition
 * @see SlideDirection.Start
 * @see enterFadeAnimationSpec
 * @see slideAnimationSpec
 * @see POP_ENTER_OFFSET_FACTOR
 */
private val TransitionScope.settingsPopEnterTransition
    get() = fadingSlideInTransition(
            towards = SlideDirection.Start,
            fadeAnimationSpec = enterFadeAnimationSpec,
            slideAnimationSpec = slideAnimationSpec,
            initialOffset = { fullWidth -> fullWidth offsetBy POP_ENTER_OFFSET_FACTOR }
    )

/**
 * Defines the "pop exit" transition for the [SettingsScreen].
 *
 * This transition is triggered when the user navigates back from the [SettingsScreen]. It creates
 * a slide-out animation towards the [end][SlideDirection.End] of the screen.
 * The screen slides out completely, moving by its full width, ensuring it is fully off-screen
 * at the end of the animation.
 *
 * This provides a standard horizontal exit effect, consistent with the app's navigation flow.
 *
 * @return [ExitTransition] The configured fading slide-out transition.
 *
 * @see fadingSlideOutTransition
 * @see POP_EXIT_OFFSET_FACTOR
 * @see EXIT_ANIMATION_DURATION
 */
private val TransitionScope.settingsPopExitTransition
    get() = fadingSlideOutTransition(
            towards = SlideDirection.End,
            fadeAnimationSpec = exitFadeAnimationSpec,
            slideAnimationSpec = slideAnimationSpec,
            targetOffset = { fullWidth -> fullWidth offsetBy POP_EXIT_OFFSET_FACTOR }
    )

/**
 * A composable that sets up the main navigation structure of the application using Jetpack Navigation.
 *
 * This function defines the navigation graph, including the different screens (routes) and the
 * transitions between them. It also initializes and provides the [MediaViewModel] to the screens
 * that need it.
 *
 * The main components managed by this [NavHost] are:
 * - [RecitersScreen]: The start destination, displaying a list of Quran reciters.
 * - [SurahsScreen]: Displays the list of Surahs for a selected reciter and moshaf. Navigation to
 * this screen requires passing `reciter` and `moshaf` objects as JSON strings.
 *
 * It wraps the [NavHost] in a [Box] that handles clearing focus when tapping outside of an
 * input field and applies appropriate padding based on the device configuration.
 *
 * This composable also integrates the [PlayerContainer], which displays the media player controls.
 * The player's state and interactions are managed via the [MediaViewModel].
 *
 * @param mediaViewModel The [MediaViewModel] to be used by the screens. If null, the default
 * [MediaViewModel] is used. It is intended to be null in production and
 * non-null in tests by [Preview] composables.
 *
 * @see NavHost
 * @see Screen
 * @see RecitersScreen
 * @see SurahsScreen
 * @see PlayerContainer
 * @see MediaViewModel
 */
@Composable
@SuppressLint("ViewModelConstructorInComposable")
fun NavGraph(mediaViewModel: MediaViewModel? = null) {
    val navController = rememberNavController()
    val mediaViewModel = mediaViewModel ?: viewModel<MediaViewModel>()

    var navBarHeightProgress by remember { mutableFloatStateOf(0f) }

    focusManager = LocalFocusManager.current

    Column(
            modifier = Modifier
                .fillMaxSize()
                .safeImePadding(currentWindowAdaptiveInfo())
                .onTouch { focusManager?.clearFocus() }
    ) {
        Box(
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceDim)
        ) {
            NavHost(
                    navController = navController,
                    startDestination = Screen.Reciters.route
            ) {
                composable(
                        route = Screen.Reciters.route,
                        enterTransition = { recitersEnterTransition },
                        exitTransition = { recitersExitTransition },
                        popEnterTransition = { recitersPopEnterTransition },
                        popExitTransition = { recitersPopExitTransition }
                ) {
                    RecitersScreen(mediaViewModel = mediaViewModel, navController = navController)
                }

                composable(
                        route = "${Screen.Surahs.route}?reciter={reciterJson}&moshaf={moshafJson}",
                        arguments = listOf(
                                navArgument("reciterJson") {
                                    type = NavType.StringType
                                    nullable = false
                                },
                                navArgument("moshafJson") {
                                    type = NavType.StringType
                                    nullable = false
                                }
                        ),
                        enterTransition = { surahsEnterTransition },
                        exitTransition = { surahsExitTransition },
                        popEnterTransition = { surahsPopEnterTransition },
                        popExitTransition = { surahsPopExitTransition }
                ) { backStackEntry ->
                    val reciter = Gson().fromJson(backStackEntry.arguments?.getString("reciterJson"), Reciter::class.java)
                    val moshaf = Gson().fromJson(backStackEntry.arguments?.getString("moshafJson"), Moshaf::class.java)

                    SurahsScreen(reciter = reciter, moshaf = moshaf, mediaViewModel = mediaViewModel)
                }

                composable(
                        route = Screen.Favorites.route,
                        enterTransition = { favoritesEnterTransition },
                        exitTransition = { favoritesExitTransition },
                        popEnterTransition = { favoritesPopEnterTransition },
                        popExitTransition = { favoritesPopExitTransition }
                ) {
                    FavoritesScreen()
                }

                composable(
                        route = Screen.Settings.route,
                        enterTransition = { settingsEnterTransition },
                        exitTransition = { settingsExitTransition },
                        popEnterTransition = { settingsPopEnterTransition },
                        popExitTransition = { settingsPopExitTransition }
                ) {
                    SettingsScreen()
                }
            }

            with(mediaViewModel) {
                PlayerContainer(
                        state = playerState,
                        onHeightChanged = { navBarHeightProgress = it },
                        onSnapped = {
                            updateState {
                                isExpanding = false
                                isMinimizing = false
                            }
                        },
                        onDragDirectionChanged = { isDraggingUp, isDraggingDown ->
                            updateState {
                                isExpanding = isDraggingUp
                                isMinimizing = isDraggingDown
                            }
                        },
                        onExpandStarted = {
                            updateState {
                                isExpanding = true
                                isMinimizing = false
                            }
                        },
                        onExpandFinished = {
                            updateState {
                                isExpanding = false
                                isMinimizing = false
                            }
                        },
                        onMinimizeStarted = {
                            updateState {
                                isExpanding = false
                                isMinimizing = true
                            }
                        },
                        onMinimizeFinished = {
                            updateState {
                                isExpanding = false
                                isMinimizing = false
                            }
                        },
                        onCloseClicked = ::closePlayer,
                        onSeekProgress = ::seekTo,
                        onSkipToPreviousSurah = ::skipToPreviousSurah,
                        onTogglePlayback = ::togglePlayback,
                        onSkipToNextSurah = ::skipToNextSurah
                )
            }
        }

        NavBar(navController = navController, navBarHeightProgress = navBarHeightProgress)
    }
}

/**
 * Creates a combined fade-in and slide-in enter transition.
 *
 * This extension function for [TransitionScope] simplifies the creation of a common navigation
 * animation where a new screen both fades in and slides into view simultaneously. It combines
 * [fadeIn] and [AnimatedContentTransitionScope.slideIntoContainer] into a single, reusable function.
 *
 * @param towards [SlideDirection] The [SlideDirection] from which the content will slide in (e.g., [SlideDirection.Up], [SlideDirection.Start]).
 * @param initialOffset [(offsetForFullSlide: Int) -> Int][initialOffset] A lambda that calculates the initial offset of the content before it starts sliding in.
 *                      The lambda receives the full slide distance (either width or height) and should return the starting pixel offset.
 *                      Defaults to the full slide distance, meaning the content starts completely off-screen.
 * @param fadeAnimationSpec [FiniteAnimationSpec] The [FiniteAnimationSpec] to use for the fade-in animation. Defaults to a standard [tween].
 * @param slideAnimationSpec [FiniteAnimationSpec] The [FiniteAnimationSpec] to use for the slide-in animation. Defaults to a standard [tween].
 *
 * @return An [EnterTransition] that combines both fading and sliding effects.
 *
 * @see fadeIn
 * @see AnimatedContentTransitionScope.slideIntoContainer
 * @see fadingSlideOutTransition
 */
private fun TransitionScope.fadingSlideInTransition(
        towards: SlideDirection,
        initialOffset: (offsetForFullSlide: Int) -> Int = { it },
        fadeAnimationSpec: FiniteAnimationSpec<Float> = tween(),
        slideAnimationSpec: FiniteAnimationSpec<IntOffset> = tween()
) = fadeIn(fadeAnimationSpec) + slideIntoContainer(towards = towards, animationSpec = slideAnimationSpec, initialOffset = initialOffset)

/**
 * Creates a combined fade-out and slide-out exit transition.
 *
 * This extension function simplifies the creation of a complex [ExitTransition] that simultaneously
 * fades out the content and slides it out of the container. It's a convenient wrapper around
 * [fadeOut] and [AnimatedContentTransitionScope.slideOutOfContainer].
 *
 * @param towards [SlideDirection] The [SlideDirection] in which the content should slide out.
 * @param targetOffset [(offsetForFullSlide: Int) -> Int][targetOffset] A lambda that calculates the target offset for the slide animation.
 *                     It receives the full dimension (width or height) of the container as an argument and should
 *                     return the final pixel offset. Defaults to the full slide distance.
 * @param fadeAnimationSpec [FiniteAnimationSpec] The [FiniteAnimationSpec] to use for the fade-out animation. Defaults to a standard [tween].
 * @param slideAnimationSpec [FiniteAnimationSpec] The [FiniteAnimationSpec] to use for the slide-out animation. Defaults to a standard [tween].
 *
 * @return An [ExitTransition] that combines fading out and sliding out.
 *
 * @see fadeOut
 * @see AnimatedContentTransitionScope.slideOutOfContainer
 * @see fadingSlideInTransition
 */
private fun TransitionScope.fadingSlideOutTransition(
        towards: SlideDirection,
        targetOffset: (offsetForFullSlide: Int) -> Int = { it },
        fadeAnimationSpec: FiniteAnimationSpec<Float> = tween(),
        slideAnimationSpec: FiniteAnimationSpec<IntOffset> = tween()
) = fadeOut(fadeAnimationSpec) + slideOutOfContainer(towards = towards, animationSpec = slideAnimationSpec, targetOffset = targetOffset)

/**
 * A [Preview] composable for the [NavGraph].
 *
 * This function sets up a preview environment for the entire navigation graph. It initializes a
 * [MediaViewModel] with sample data ([sampleReciters] and [sampleSurahs]) to simulate a real-world
 * scenario. This allows for visual testing of the different screens and their layouts within the
 * Android Studio preview pane.
 *
 * The `@Preview(locale = "ar")` annotation configures the preview to render with an Arabic locale,
 * which is useful for testing right-to-left (RTL) layouts. The `@SuppressLint` is used to suppress
 * the warning about creating a ViewModel directly in a composable, which is acceptable in a preview context.
 *
 * @see NavGraph
 * @see MediaViewModel
 * @see Preview
 */
@Composable
@Preview(locale = "ar")
@SuppressLint("ViewModelConstructorInComposable")
fun NavGraphPreview() {
    val mobileApp = MobileApplication()
    val mediaViewModel = MediaViewModel(mobileApp).apply {
        updateState {
            reciters = sampleReciters
            surahs = sampleSurahs
        }
    }
    NavGraph(mediaViewModel)
}
