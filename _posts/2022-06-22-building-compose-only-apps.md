---
title: Building Compose only apps
layout: post
thumbnail: /static/thumbnail/2022-06-22-building-compose-only-apps.jpg

external: false

categories: post
tags:
  - Android
  - Compose
---

This guide assumes you’re already familiar with the basics of Compose. If not
you can check out [this guide which provides a quick intro](/post/2022/06/21/a-quick-intro-to-jetpack-compose.html),
or check out [the official tutorials](https://developer.android.com/jetpack/compose).

Useful read might be the [API Guidelines for Jetpack Compose](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-api-guidelines.md).

Content:

- [The Android lifecycle](#the-android-lifecycle)
- [Navigation](#navigation)
- [Creating a Screen in Compose](#creating-a-screen-in-compose)
- [Theming](#theming)

### The Android lifecycle

As you know the activity is recreated on configuration change. This means
that you’ll lose all your Compose UI including all the values cached by
`remember`, which isn’t something we want since Compose, thanks to the way
[recomposition](https://developer.android.com/jetpack/compose/mental-model#recomposition)
works, will keep the UI parts that are the same.

To avoid this and to let Compose handle configuration changes, add in the
manifest the configuration changes you want to be handled by the activity using
`android:configChanges`.

### Navigation

For Compose we have an adaptation of the Jetpack Navigation library, which is
not as mature as the fragment equivalent, since it lacks safe args and animations.

`androidx.navigation:navigation-compose`

For animations, we have [the accompanist version](https://github.com/google/accompanist#navigation-animation) of navigation-compose:

```kotlin
@Composable
fun AppNavigation(
    state: NavigationState = rememberNavigationState()
) {
    val navController: NavHostController = rememberAnimatedNavController()

    AnimatedNavHost(
        navController,
        startDestination = Route.Splash,
        modifier = Modifier.background(AppTheme.colors.surface)
    ) {
        composable(Route.Splash) {
            Spacer(modifier = Modifier.fillMaxSize())
        }
        composable(Route.Login) { // the default transition is cross fade
            LoginScreen()
        }
        composable(
            route = Route.Home,
            enterTransition = { ... }, // see docs for transition usage
            exitTransition = { ... },
        ) {
            HomeScreen()
        }
    }

    when (state.loggedIn) {
        true -> navController.onLogin()
        false -> navController.onLogout()
        null -> {
        }
    }
}

private fun NavHostController.onLogout() {
    navigate(Route.Login) {
        popUpTo(this@onLogout.graph.id) {
            inclusive = true
        }
    }
}

private fun NavHostController.onLogin() {
    navigate(Route.Home) {
        popUpTo(this@onLogin.graph.id) {
            inclusive = true
        }
    }
}
```

If you want safe args you could take a look at Decompose and this guide,
but Jetpack navigation is more appropriate for now as a first step into
the Compose world.

#### Navigation and ViewModels

An important note for Jetpack Navigation is the fact that ViewModels are
scoped to the destination, which would otherwise be scoped to the host
activity or fragment.

### Creating a screen in Compose

In Compose there’s no concept of a screen. So by screen, we mean a part
of UI implemented by a Composable that would otherwise be represented by
a Fragment.

So we’ll simply apply the best practices of creating a Composable function
with state, and below we’ll see how a given Fragment with ViewModel is
translated in Compose.

First, the Fragment simply becomes a @Composable. We’ll keep the ViewModel
for now, which will be injected by Koin:

```kotlin
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = getViewModel(),
) {
    Box(modifier.fillMaxSize()) {
    }
}
```

Note that, if you’re using ViewModel you are depending on Jetpack Navigation
to have it scoped as expected to the screen (to the destination).

It’s important to have the ViewModel or State Holder as a parameter so that
the parent of your Composable is able to read the state and send events to it.

#### State Holder

While `ViewModel` is familiar, we can further decouple our UI state from
Android and implement a
[state holder idiomatic to Compose](https://developer.android.com/codelabs/jetpack-compose-advanced-state-side-effects?continue=https%3A%2F%2Fdeveloper.android.com%2Fcourses%2Fpathways%2Fcompose%23codelab-https%3A%2F%2Fdeveloper.android.com%2Fcodelabs%2Fjetpack-compose-advanced-state-side-effects#5).

So our `ViewModel` becomes a plain Kotlin class that doesn’t extend anything.
The state holder should be `remember`ed so that it is saved across recompositions.

```kotlin
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    state: LoginScreenState = remember { LoginScreenState() },
) {
}
```

To follow the pattern used in Compose, and to hide default/injected values into
the state holder, we should have the following function for every state holder:

```kotlin
@Composable
fun rememberLoginScreenState() = remember { LoginScreenState() }

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    state: LoginScreenState = rememberLoginScreenState(),
) {
}
```

#### CoroutineScope

At this point, you might notice that the state holder is missing the useful
`viewModelScope`. We can add a coroutine scope that follows the lifecycle of
the screen as follows:

```kotlin
@Composable
fun rememberLoginScreenState(
	stateScope: CoroutineScope = rememberCoroutineScope(),
) = remember { LoginScreenState(stateScope) }
```

This coroutine scope will be canceled when the composable exits the
composition.

Note that the cancelation is different than that of `viewModelScope`.
`stateScope` in the example above will be canceled when the screen is in the
navigation back stack, while `viewModelScope` is not.

#### Dependency Injection

We usually have dependencies injected into the constructor. In Compose the
custom remember function is responsible for this:

```kotlin
@Composable
fun rememberLoginScreenState(
    stateScope: CoroutineScope = rememberCoroutineScope(),
    doLogin: DoLoginUseCase = get(), // injected with Koin
) = remember { LoginScreenState(stateScope, doLogin) }
```

#### LiveData vs MutableState

We can also replace any LiveData to Compose’s MutableState, like this:

```kotlin
private val _emai = MutableLiveData<String>("")
val email: LiveData<String> = _email

private val _password = MutableLiveData<String>("")
val password: LiveData<String> = _password

val loginEnabled = combineLiveData(email, password) { email, password ->
    email.isNotEmpty() && password.isNotEmpty()
}

private val _emai = MutableLiveData<String>("")
val email: LiveData<String> = _email

private val _password = MutableLiveData<String>("")
val password: LiveData<String> = _password

val loginEnabled = combineLiveData(email, password) { email, password ->
		email.isNotEmpty() && password.isNotEmpty()
}
```

#### State Restoration

For types that are not automatically saved into a Bundle, like our state holder,
we need to create a Saver.

```kotlin
@Composable
fun rememberLoginScreenState(
    stateScope: CoroutineScope = rememberCoroutineScope(),
    doLogin: DoLoginUseCase = get(),
): LoginScreenState {
    return rememberSaveable(saver = LoginScreenState.getSaver(stateScope, doLogin)) {
        LoginScreenState(stateScope, doLogin)
    }
}

class LoginScreenState(
    private val stateScope: CoroutineScope,
    private val doLogin: DoLoginUseCase,
) {

    var email: String by mutableStateOf("")
        private set

    var password: String by mutableStateOf("")
        private set

    val loginEnabled by derivedStateOf { email.isNotEmpty() && password.isNotEmpty() }

		fun onEmail(text: String) {
        email = text.trim()
    }

    fun onPassword(text: String) {
        password = text
    }

    companion object {
        private const val EMAIL: String = "EMAIL"
        private const val PASS: String = "PASS"

        fun getSaver(
            stateScope: CoroutineScope,
            login: DoLoginUseCase,
        ): Saver<LoginScreenState, *> = mapSaver(
            save = { mapOf(EMAIL to it.email, PASS to it.password) },
            restore = {
                LoginScreenState(stateScope, login).apply {
                    onEmail(it.getOrElse(EMAIL) { "" } as String)
                    onPassword(it.getOrElse(PASS) { "" } as String)
                }
            }
        )
    }
}
```

### Theming

Since theming moved from XML into Kotlin, we can work a bit easier with it.

Just as before we have the Material Theme with its components available, and
by default, you can rely on that.

But the recommendation is to create your own Theme with attributes that should
ideally match exactly what it is in the design.

Theming in Compose relies heavily on `CompositionLocal` (see
[this short guide](/post/2022/06/21/a-quick-intro-to-jetpack-compose.html#compositionlocal)
or the
[official guide](https://developer.android.com/jetpack/compose/compositionlocal)).

If we’re taking as an example colors, we need to create a `data class` for all the color attributes:

```kotlin
@Immutable
data class Colors(
    val primary: Color = Color(0xFF5a41fa),
    val primaryVariant: Color = Color(0xFF4834c8),
    val accent: Color = Color(0xFFffe650),
    val accentVariant: Color = Color(0xffccb840),
    val surface: Color = Color(0xFFFFFFFF),
    val surfaceDisabled: Color = Color(0xFFE7E8EA),
    val textOnSurface: Color = Color(0xFF0D1C2E),
    val textOnPrimary: Color = Color(0xFFFFFFFF),
    val hint: Color = Color(0x66000000),
    val error: Color = Color(0xFFFF1F00),
)
```

Then to create a CompositionLocal for it:

```kotlin
internal val LocalColors = staticCompositionLocalOf { lightColors }

val lightColors: Colors = Colors(...)
val darkColors: Colors = Colors(...)
```

And finally, create a composable that provides the Theme values:

```kotlin
@Composable
fun MyTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) darkColors else lightColors
    CompositionLocalProvider(
        LocalColors provides colors,
        // other theme Locals,
        content = content
    )
}
```

Aditionally, to keep the API similarity with MaterialTheme create an
object that provides easy access to theme attributes:

```kotlin
object AppTheme {
    val dimens: Dimens
        @Composable
        @ReadOnlyComposable
        get() = LocalDimens.current

    val colors: Colors
        @Composable
        @ReadOnlyComposable
        get() = LocalColors.current

    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = LocalTypography.current

    val shapes: Shapes
        @Composable
        @ReadOnlyComposable
        get() = LocalShapes.current
}
```

#### Theming and configurations

You can provide different values depending on configuration by reading the
configuration from LocalConfiguration in your theme composable.

#### Catalog

The catalog should contain the styled components identified in the design.
Just as `MaterialTheme` comes with its set of components, we should also
create our components that match our design, so that they are ready to be
used in the app. Applying styles as needed outside of the catalog will
create inconsistencies.

Even if your components are just material components stylized, you should
still wrap them in your own components. In this case, only your catalog should
have material imports and your screens should only have catalog or foundation
imports.

Applying the same principle to typography you should create composables for
your texts such as `Title()`, `Subtitle()`, `Body()`, and you should never use
`Text()` directly.

---

Thanks to [Gergely Hegedüs](https://halcyonmobile.com/blog/author/gergo/) for reviewing.

---
