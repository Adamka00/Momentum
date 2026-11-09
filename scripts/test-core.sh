#!/usr/bin/env bash
# Dependency-light checks while the Fabric plugin repository is unavailable.
# This deliberately does not validate Minecraft, networking, graphics, or a release JAR.
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/core-test-libs build/core-tests
python3 - <<'PY'
from pathlib import Path
from urllib.request import urlretrieve
root = Path('build/core-test-libs')
for artifact in (
    'org/junit/platform/junit-platform-console-standalone/1.13.4/junit-platform-console-standalone-1.13.4.jar',
    'com/google/code/gson/gson/2.13.2/gson-2.13.2.jar',
    'org/slf4j/slf4j-api/2.0.17/slf4j-api-2.0.17.jar',
):
    destination = root / artifact.rsplit('/', 1)[1]
    if not destination.exists():
        temporary = destination.with_suffix('.download')
        urlretrieve('https://repo.maven.apache.org/maven2/' + artifact, temporary)
        temporary.replace(destination)
PY
run_dir=$(mktemp -d build/core-tests/run-XXXXXXXX)
javac --release 25 -cp 'build/core-test-libs/*' -d "$run_dir/classes" \
  src/main/java/hu/adamka00/momentum/config/*.java \
  src/main/java/hu/adamka00/momentum/movement/MovementState.java \
  src/main/java/hu/adamka00/momentum/movement/MovementMath.java \
  src/main/java/hu/adamka00/momentum/movement/SlideState.java \
  src/main/java/hu/adamka00/momentum/movement/WallJumpState.java \
  src/main/java/hu/adamka00/momentum/movement/LongJumpState.java \
  src/test/java/hu/adamka00/momentum/*.java
java -jar build/core-test-libs/junit-platform-console-standalone-1.13.4.jar execute \
  --class-path "$run_dir/classes:build/core-test-libs/gson-2.13.2.jar:build/core-test-libs/slf4j-api-2.0.17.jar" \
  --scan-class-path --fail-if-no-tests --reports-dir "$run_dir/results" --details=summary
