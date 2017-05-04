#!/usr/bin/env bash

set -e

rm -rf build

./gradlew -q clean check install --stacktrace

integration-test-app/run_integration_tests.sh

cd functional-test-app

./gradlew check

cd ..

if [[ $TRAVIS_BRANCH == 'master' && $TRAVIS_PULL_REQUEST == 'false' ]]; then

    if [[ -n $TRAVIS_TAG ]]; then

        ./gradlew bintrayUpload --stacktrace

        ./publish-docs.sh

    fi

fi
