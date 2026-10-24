+++
title = "Installation"
description = "Requirements and how to run KebabEditor."
weight = 20
template = "docs/page.html"

[extra]
lead = 'You need JDK 25 and a GPU with OpenGL 3.x support.'
toc = true
top = false
+++

## Requirements

- JDK 25. Gradle can download a matching toolchain.
- A GPU and driver with OpenGL 3.x support. Without it the 3D viewport shows an explanation instead.

## Run from the command line

```sh
./gradlew run
```

## Run the tests

```sh
./gradlew test
```

## Run from an IDE

If you start the application from an IDE instead of Gradle, copy the JVM arguments from `applicationDefaultJvmArgs` in `app/build.gradle.kts`. JOGL needs them to reach native libraries and AWT internals.
