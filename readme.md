# Tetris Mouse

A Tetris clone developed specifically for playing on the Steelseries Rival 700 gaming mouse, proving that it is a true gaming platform in the same league as an Oculus Rift or HTC Vive.

Like a VR headset, the Rival 700 mouse relies on you having a powerful computer to do the heavy lifting of executing the game whilst the mouse itself takes care of displaying the game, reading user input, and providing tactile feedback.

## Gameplay video

[![Thumbnail for demonstration video on YouTube](http://img.youtube.com/vi/tr0I15GGhJk/0.jpg)](http://www.youtube.com/watch?v=tr0I15GGhJk)

## Building & running

To build & run the game from the CLI:

`./gradlew run`

There are additional standard Gradle commands that do other things... You can look them up yourself.

Requirements to run:

- Java. I've been using [OpenJDK](https://adoptopenjdk.net/) 13.
- An OS supported by [JNativeHook](https://github.com/kwhat/jnativehook) (used to get mouse input without having a window open)
- Steelseries Engine/Steelseries GG installed & running.

## Gameplay

The controls are:

| Input | Action |
|-------|--------|
| Left click | Move piece left |
| Right click |Move piece right |
| Mouse wheel up | Rotate piece |
| Mouse wheel down | Soft drop piece |

Features include:

- Level & speed increase as you clear more lines
- Scoring based on Game Boy version
- Soft dropping
- Proper [Super Rotation System](https://strategywiki.org/wiki/Tetris/Rotation_systems) style rotations (I think. Pinch of salt)
- Tactile feedback with vibrations at key gameplay moments
- Artisanal font

## Think some code is messy?

Cut me some slack! I'm the first video game developer to release a game for the Rival 700 platform. _Also_, it's just a fun joke I very sporadically worked on.