#!/bin/bash
set -e
rm -rf *.zip

# all this is doing is compiling, work in progres...
./gradlew clean compileGroovy

EXIT_STATUS=0

exit $EXIT_STATUS
