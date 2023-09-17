---
title: A quick intro to Jetpack Compose
layout: post
thumbnail: /static/thumbnail/2022-06-21-a-quick-intro-to-jetpack-compose.jpg

external: false

categories: post
tags:
  - Android
  - Compose
---

This document shortly introduces the “primitives” of Compose. For a more
detailed guide see
[the official documentation](https://developer.android.com/jetpack/compose).

### @Composable and recompositions

The Composable function is the building block for working with Compose.
Compose tracks your Composables and injects code into them that achieves
state management and recomposition. So when building UI in Compose you
only define its structure, but Compose decides when to call your Composables.

```kotlin
// Structure:
@Composable
fun LoginScreen() {
    Column {
        UsernameField()
        PasswordField()
    }
}
```

Your composable is called when it first enters the composition (when it first
appears on the screen) and it will also be called whenever the state it
depends on changes, known as recomposition.

The important takeaway out of recomposition is that you shouldn’t make
assumptions about when your functions are called. In the example above
`PasswordField()` may be recomposed and `UsernameField()` skipped.

Instead, Compose provides hooks into the lifecycle of a Composable called
[effects](https://developer.android.com/jetpack/compose/side-effects).

### remember()

Since your composable is called any number of times, you will often need
to cache values across recomposition.

This is what `remember` achieves:

```kotlin
val myStateHolder = remember { MyStateHolder() }
```

Its block will only be called when it first enters the composition.
On recomposition it will return the same value.

You may invalidate the cached value by providing one or more key arguments:

```kotlin
val myStateHolder = remember(id) { MyStateHolder(id) }
```

This means that on recomposition only if the `id` changes it will call again the
block.

### mutableStateOf()

You’ll often create a `mutableStateOf` in the block of your `remember`:

```kotlin
var loading by remember { mutableStateOf(false) }

if (loading) {
    LoadingIndicator()
} else {
    Button(onClick = { loading = true }) {
        Text("Click me!")
    }
}
```

`mutableStateOf` creates a state that is tracked by compose, meaning that
assigning a new value to it triggers a recomposition.

This means that it is actually an observable value similar to LiveData when
used in data binding.

Whenever you’re reading its value you're actually observing the value, but
without having to call observe.

Since we’re comparing state with LiveData, a useful mention is that the
equivalent of `Transformations.map(dependantLiveData) { }` is `derivedStateOf`:

```kotlin
var email: String by remember { mutableStateOf("") }
var password: String by remember { mutableStateOf("") }

val loginEnabled by remember { derivedStateOf { email.isNotEmpty() && password.isNotEmpty() } }
```

Notice how it automatically depends on captured state instead of having to
specify on what it depends.

### Basic layouts

Here are the common layouts and their View equivalents:

- FrameLayout → Box
- LinearLayout → Column, Row
- ConstraintLayout → [ConstraintLayout](https://developer.android.com/jetpack/compose/layouts/constraintlayout) (the compose api)
- RecyclerView → [LazyColum, LazyRow](https://developer.android.com/jetpack/compose/layouts/constraintlayout)

```kotlin
// LazyColumn example:
LazyColumn {
    items(myList, key = { it.id }) {
        Text("${it.someField}")
    }
}
```

### Modifiers

When you need to alter how a composable looks or behaves, you will often use
a Modifier.

All available layouts accept modifiers and it is a good practice to have a
modifier parameter when you're defining your custom layouts.

```kotlin
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(AppTheme.colors.surface)
    ) {
    // ...
    }
}
```

Keep in mind that the order of modifiers matter:

```kotlin
Box(
    modifier = Modifier
        .padding(16.dp)
        .clickable { /* ... */ }
        .padding(16.dp)
) {
    // ...
}
```

In the example above the clickable area doesn’t include the first padding,
but includes the second padding.

### CompositionLocal

Locals are tree-scoped values that can be accessed at any time in a composable.

Let’s explain what that means. The UI is a tree-like structure in which our
composables are the nodes[^1].

At any time in that tree we can access a local like this:

```kotlin
@Composable
fun Foo() {
    val context = LocalContext.current
}
```

A `CompositionLocal` is _tree-scoped_ because the value it contains may be
different based on the location in the UI tree. When assigning a value to a
local (using `CompositionLocalProvider`) its value is changed only in the
subtree of that node.

The best use case for this is theming. You could achieve a theme overlay by
providing different values like this:

```kotlin
val lightColors = LocalColors.current // here it returns the old color values
CompositionLocalProvider(LocalColors provides darkColors) {
    // new color values are returned in the subtree starting from this node
    val darkColorsHere = LocalColors.current
}
val stillLightColors = LocalColors.current // here it still returns the old color values
```

You can create your own Local using `staticCompositionLocalOf` like this:

```kotlin
    val LocalColors = staticCompositionLocalOf { lightColors }
```

### Side effects

Side effects are hooks into the lifecycle of a composable. In short:

- `LaunchedEffect` is used for running code when it enters the composition.
- `SideEffect` is used for running code on composition and subsequent recompositions.
- `DisposableEffect` is used for running code when it leaves the composition.

For a far better explanation see [the official documentation](https://developer.android.com/jetpack/compose/side-effects).

### rememberSaveable()

While `remember` saves values across recompositions, `rememberSaveable` saves
values also across configuration changes and process recreation. The
requirement is that the values need to be serializable into a bundle.

For types that can’t be saved automatically into a Bundle, you can either
make the type parcelable, or you could create a `Saver`:

```kotlin
data class City(val name: String, val country: String)

val CitySaver = run {
    val nameKey = "Name"
    val countryKey = "Country"
    mapSaver(
        save = { mapOf(nameKey to it.name, countryKey to it.country) },
        restore = { City(it[nameKey] as String, it[countryKey] as String) }
    )
}

@Composable
fun CityScreen() {
    var selectedCity = rememberSaveable(stateSaver = CitySaver) {
        mutableStateOf(City("Madrid", "Spain"))
    }
}
```

Read more about rememberSaveable in [the official docs](https://developer.android.com/jetpack/compose/state#restore-ui-state).

---

Thanks to [Gergely Hegedüs](https://halcyonmobile.com/blog/author/gergo/) for reviewing.

---

[^1]:
    Technically not accurate, but for the sake of simplicity, we can consider the composables as the nodes.
    The actual nodes are LayoutNodes, but you don’t necessarily need this detail when working in Compose day to day.
    If you’re interested though, you can check out this article that dives into the internals: https://jorgecastillo.dev/diving-into-mosaic
