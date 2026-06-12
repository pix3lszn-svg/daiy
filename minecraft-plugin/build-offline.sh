#!/usr/bin/env bash
# Offline-ish build: compiles the plugin against the Paper API *source*
# (official PaperMC GitHub) instead of the prebuilt artifact, using only
# Maven Central + GitHub. Used where repo.papermc.io is unreachable.
# Requires: JDK 21, git, curl.
set -euo pipefail
cd "$(dirname "$0")"

WORK=${WORK:-/tmp/emeraldperks-build}
PAPER_BRANCH=${PAPER_BRANCH:-ver/1.21.11}
mkdir -p "$WORK/deps"

# 1. Paper API source
if [ ! -d "$WORK/paper" ]; then
  git clone --depth 1 --branch "$PAPER_BRANCH" --filter=blob:none --sparse \
    https://github.com/PaperMC/Paper "$WORK/paper"
  git -C "$WORK/paper" sparse-checkout set paper-api
fi

# 2. Brigadier built from Mojang's source (not on Maven Central)
if [ ! -f "$WORK/deps/brigadier-src.jar" ]; then
  git clone --depth 1 https://github.com/Mojang/brigadier "$WORK/brigadier"
  mkdir -p "$WORK/brig-out"
  javac --release 21 -nowarn -cp "$WORK/deps/guava-33.3.1-jre.jar" \
    -d "$WORK/brig-out" $(find "$WORK/brigadier/src/main/java" -name '*.java') || true
fi

# 3. Compile-time deps from Maven Central (versions from paper-api's build.gradle.kts)
fetch() { # group/path artifact version
  local f="$WORK/deps/$2-$3.jar"
  [ -f "$f" ] || curl -fsS -o "$f" "https://repo1.maven.org/maven2/$1/$2/$3/$2-$3.jar"
}
fetch com/google/guava guava 33.3.1-jre
fetch com/google/code/gson gson 2.11.0
fetch org/yaml snakeyaml 2.2
fetch org/joml joml 1.10.8
fetch it/unimi/dsi fastutil 8.5.15
fetch org/apache/logging/log4j log4j-api 2.24.1
fetch org/slf4j slf4j-api 2.0.16
for a in adventure-api adventure-key adventure-text-minimessage \
         adventure-text-serializer-gson adventure-text-serializer-legacy \
         adventure-text-serializer-plain adventure-text-logger-slf4j; do
  fetch net/kyori "$a" 4.26.1
done
fetch net/kyori examination-api 1.3.0
fetch net/kyori examination-string 1.3.0
fetch net/kyori option 1.1.0
fetch org/jetbrains annotations 26.0.2
fetch org/checkerframework checker-qual 3.49.2
fetch org/jspecify jspecify 1.0.0
fetch net/md-5 bungeecord-chat 1.21-R0.4
for a in maven-resolver-api maven-resolver-spi maven-resolver-util \
         maven-resolver-impl maven-resolver-connector-basic maven-resolver-transport-http; do
  fetch org/apache/maven/resolver "$a" 1.9.18
done
for a in maven-resolver-provider maven-model maven-model-builder \
         maven-artifact maven-repository-metadata; do
  fetch org/apache/maven "$a" 3.9.6
done

# (re)build brigadier now that guava is present
if [ ! -f "$WORK/deps/brigadier-src.jar" ]; then
  mkdir -p "$WORK/brig-out"
  javac --release 21 -nowarn -cp "$WORK/deps/guava-33.3.1-jre.jar" \
    -d "$WORK/brig-out" $(find "$WORK/brigadier/src/main/java" -name '*.java')
  jar cf "$WORK/deps/brigadier-src.jar" -C "$WORK/brig-out" .
fi

# 4. Compile the plugin. -implicit:none keeps Paper API classes out of the jar.
CP=$(ls "$WORK"/deps/*.jar | tr '\n' ':')
rm -rf target/classes && mkdir -p target/classes
javac --release 21 -encoding UTF-8 -implicit:none -proc:none \
  -sourcepath "$WORK/paper/paper-api/src/main/java:$WORK/paper/paper-api/src/generated/java" \
  -cp "$CP" -d target/classes $(find src/main/java -name '*.java')

# 5. Run the Patreon parser smoke tests
mkdir -p target/test
javac --release 21 -cp "target/classes:$WORK/deps/gson-2.11.0.jar" \
  -d target/test src/test/java/PatreonClientTest.java
java -cp "target/test:target/classes:$WORK/deps/gson-2.11.0.jar" PatreonClientTest

# 6. Package
cp src/main/resources/plugin.yml src/main/resources/config.yml target/classes/
jar cf target/EmeraldPerks-1.2.0.jar -C target/classes .
echo "Built target/EmeraldPerks-1.2.0.jar"
