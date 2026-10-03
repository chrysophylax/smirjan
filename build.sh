#!/usr/bin/env bash
# Builds build/smirjan.jar with nothing but the JDK. `./build.sh test` also runs the self-tests.
set -euo pipefail
cd "$(dirname "$0")"

rm -rf build
mkdir -p build/classes build/test-classes
javac --release 25 -Xlint:all -Werror -d build/classes $(find src -name '*.java')
# Resources (the bundled meaning lists) go next to the classes.
(cd resources && find . -type f -exec install -D -m 644 {} ../build/classes/{} \;)
jar --create --file build/smirjan.jar --main-class smirjan.Main -C build/classes .

if [[ "${1:-}" == "test" ]]; then
    javac --release 25 -Xlint:all -Werror -cp build/classes -d build/test-classes $(find test -name '*.java')
    java -cp build/classes:build/test-classes smirjan.SelfTest
fi

if [[ "${1:-}" == "test" ]]; then
    # Every shipped example must still parse and generate.
    for def in examples/*.def docs/examples/*.def; do
        status=0
        java -jar build/smirjan.jar "$def" 5 > /dev/null 2> build/example.err || status=$?
        # Status 3 (fewer distinct words than asked) is fine for tiny examples.
        if [[ $status -ne 0 && $status -ne 3 ]]; then
            echo "example failed: $def" >&2
            cat build/example.err >&2
            exit 1
        fi
    done
    echo "all examples run"
fi
