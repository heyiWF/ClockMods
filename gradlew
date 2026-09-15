#!/bin/sh

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# Keep build caches local to this project unless explicitly overridden.
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$APP_HOME/.gradle/user-home}"
export ANDROID_USER_HOME="${ANDROID_USER_HOME:-$APP_HOME/.gradle/android-user-home}"

if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD=java
fi

exec "$JAVACMD" -Xmx64m -Xms64m $JAVA_OPTS $GRADLE_OPTS -Dorg.gradle.appname=gradlew -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
