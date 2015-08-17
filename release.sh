rm -rf build/release
mkdir -p build/release
cd build/release
git clone git@github.com:grails-plugins/grails-spring-security-acl.git
cd grails-spring-security-acl
grails clean
grails compile

gradle bintrayUpload --stacktrace
