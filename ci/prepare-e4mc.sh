#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
MAVEN=".e4mc-vendor/link/e4mc/e4mc-forge/6.2.3"
mkdir -p "$MAVEN"
JAR="$MAVEN/e4mc-forge-6.2.3.jar"
if [ ! -s "$JAR" ]; then
  PRIMARY='https://cursemaven.com/curse/maven/e4mc-849519/9030240/e4mc-849519-9030240.jar'
  FALLBACK='https://api.modrinth.com/maven/maven/modrinth/qANg5Jrr/VYizGDdc/qANg5Jrr-VYizGDdc.jar'
  echo "Downloading official MIT-licensed e4mc Forge 6.2.3..."
  if ! curl -fLSs --retry 3 --max-time 90 -o "$JAR" "$PRIMARY"; then
    curl -fLSs --retry 3 --max-time 90 -o "$JAR" "$FALLBACK"
  fi
fi
python3 - "$JAR" <<'PY'
import sys,zipfile,os,hashlib
p=sys.argv[1]
if not (100000 <= os.path.getsize(p) <= 15000000):
    raise SystemExit("e4mc JAR size is outside expected bounds")
with zipfile.ZipFile(p) as z:
    mods=z.read("META-INF/mods.toml").decode('utf-8')
    import re
    assert re.search(r'modId\s*=\s*"e4mc"', mods), 'Unexpected mod ID'
digest = hashlib.sha256(open(p,'rb').read()).hexdigest()
expected = '3313a4087661a2678dcf0c26f45bb5a44aed45ad7a36b86878f60bbe1defa6b3'
assert digest == expected, f'Unrecognized upstream e4mc JAR SHA256: {digest}'
print("Verified e4mc 6.2.3 upstream SHA256:", digest)
PY
cat > "$MAVEN/e4mc-forge-6.2.3.pom" <<'EOF'
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>link.e4mc</groupId>
  <artifactId>e4mc-forge</artifactId>
  <version>6.2.3</version>
  <packaging>jar</packaging>
</project>
EOF
cat > ".e4mc-vendor/link/e4mc/e4mc-forge/maven-metadata.xml" <<'EOF'
<metadata>
  <groupId>link.e4mc</groupId>
  <artifactId>e4mc-forge</artifactId>
  <versioning>
    <latest>6.2.3</latest>
    <release>6.2.3</release>
    <versions><version>6.2.3</version></versions>
    <lastUpdated>20261001000000</lastUpdated>
  </versioning>
</metadata>
EOF
echo "e4mc staged for Gradle Forge JarJar."
