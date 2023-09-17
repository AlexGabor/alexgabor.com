---
title: Scrolling a header above a RecyclerView with MotionLayout
layout: post
thumbnail: /static/thumbnail/2019-08-19-scrolling-a-header-above-a-recyclerview-with-motionlayout.png

external: true
external_link: https://medium.com/halcyon-mobile/scrolling-a-header-above-a-recyclerview-with-motionlayout-5f36ebaa3b5f

categories: post
tags:
  - Android
---

![Left: Scroll without MotionLayout. Right: Scroll with MotionLayout](https://miro.medium.com/v2/resize:fit:640/1*ACoGxbjIoQsrQS6OBrh1vQ.gif)

It usually happens that we as Android developers have to implement a screen
that consists of some header and a list. With ConstraintLayout that’s pretty
easy. Some TextViews, some ImageViews, a RecyclerView, add a few constraints
and Bam! the screen is ready.

Except… The header doesn’t scroll, only the items in the RecyclerView.

Your header may be as complex as you want, but for the sake of simplicity it
will be only an ImageView in this example.

```xml
<androidx.constraintlayout.widget.ConstraintLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <ImageView
        android:id="@+id/header"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/list"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@id/header" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

### Potential solutions

At this point you might be tempted to put everything in a NestedScrollView,
but this solution has a hidden flaw. The RecyclerView will expand to its
maximum height outside the screen and it will practically loose its
recycling behavior.

Another solution would be to include the header in the RecyclerView, but this
over complicates things since the adapter needs to be notified of any change
you might want to do to the header. Also it might turn a simple adapter into
one that has to support multiple item types.

### MotionLayout

The solution that I find to be the most convenient is to use MotionLayout.
MotionLayout is a subclass of ConstraintLayout that will be included in the
upcoming 2.0.0 version of ConstraintLayout.

To implement the desired scrolling behavior, the first thing you need to do
is replace ConstraintLayout with MotionLayout and then specify a scene file.

```xml
<androidx.constraintlayout.motion.widget.MotionLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:layoutDescription="@xml/scene">
```

The starting state is what was initially in the layout file. So that’s easy,
just copy and paste the attributes of the header in the scene file.

```xml
<ConstraintSet android:id="@+id/start">
    <Constraint
        android:id="@+id/header"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        motion:layout_constraintEnd_toEndOf="parent"
        motion:layout_constraintStart_toStartOf="parent"
        motion:layout_constraintTop_toTopOf="parent" />
</ConstraintSet>
```

For the end state, you need to change the constraints of the header so that
instead of its top being constrained to the top of the parent, now its bottom
is constrained to top of the parent so that it will go out of the screen.

```xml
<ConstraintSet android:id="@+id/end">
    <Constraint
        android:id="@+id/header"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        motion:layout_constraintEnd_toEndOf="parent"
        motion:layout_constraintStart_toStartOf="parent"
        motion:layout_constraintBottom_toTopOf="parent" />
</ConstraintSet>
```

Now for the transition, we want it to happen on swipe when dragging up. The
default behavior of MotionLayout is to end in its closest state. In this case,
we want to change it so that it may stop in an intermediary state if needed.

```xml
<Transition
    motion:constraintSetEnd="@+id/end"
    motion:constraintSetStart="@+id/start">
    <OnSwipe
        motion:onTouchUp="stop"
        motion:dragDirection="dragUp"
        motion:touchAnchorId="@+id/header" />
</Transition>
```

And that’s it.

You can find the complete project on GitHub:
[MotionPlaygorund](https://github.com/AlexGabor/MotionPlayground?source=post_page-----5f36ebaa3b5f--------------------------------)

MotionLayout is capable of more than simulating the behavior of the other
solutions. See Google’s examples on [GitHub](https://github.com/googlesamples/android-ConstraintLayoutExamples)
and on [Medium](https://medium.com/google-developers/introduction-to-motionlayout-part-i-29208674b10d).

Note that MotionLayout is still in beta, but you can keep an eye on updates
[here](https://developer.android.com/jetpack/androidx/releases/constraintlayout)
and leave feedback on the [issue tracker](https://issuetracker.google.com/issues/new?component=323867&template=1023345).
