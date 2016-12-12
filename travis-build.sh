#!/bin/bash
set -e
rm -rf *.zip

./gradlew clean compileGroovy

EXIT_STATUS=0

echo "Publishing archives for branch $TRAVIS_BRANCH"
if [[ -n $TRAVIS_TAG ]] || [[ $TRAVIS_BRANCH == 'master' && $TRAVIS_PULL_REQUEST == 'false' ]]; then

  if [[ -n $TRAVIS_TAG ]]; then
      ./gradlew bintrayUpload || EXIT_STATUS=$?
  else
      ./gradlew publish || EXIT_STATUS=$?
  fi

  ./gradlew docs || EXIT_STATUS=$?

  git config --global user.name "$GIT_NAME"
  git config --global user.email "$GIT_EMAIL"
  git config --global credential.helper "store --file=~/.git-credentials"
  echo "https://$GH_TOKEN:@github.com" > ~/.git-credentials

  git clone https://${GH_TOKEN}@github.com/${TRAVIS_REPO_SLUG}.git -b gh-pages gh-pages --single-branch > /dev/null
  cd gh-pages

  # If this is the master branch then update the snapshot
  if [[ $TRAVIS_BRANCH == 'master' ]]; then
     rm -rf snapshot
     mkdir -p snapshot
     cp -r ../build/docs/index.html ./snapshot/
     cp -r ../build/docs/*.epub ./snapshot/
     cp -r ../build/docs/*.pdf ./snapshot/

     git add snapshot/*
  fi

  # If there is a tag present then this becomes the latest
  if [[ -n $TRAVIS_TAG ]]; then

      version = ${TRAVIS_TAG:0:2}
      mkdir -p "${version}"

      cp -r ../build/docs/ghpages.html ./index.html
      cp -r ../build/docs/index.html ./$version/
      cp -r ../build/docs/*.epub ./$version/
      cp -r ../build/docs/*.pdf ./$version/

      git add index.html
      git add "$version/*"

  fi

  git commit -a -m "Updating docs for Travis build: https://travis-ci.org/$TRAVIS_REPO_SLUG/builds/$TRAVIS_BUILD_ID"
  git push origin HEAD
  cd ..
  rm -rf gh-pages
fi

exit $EXIT_STATUS
