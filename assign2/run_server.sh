./gradlew build
cd build/classes/java/main || exit
rmiregistry & sleep 1 && java server.Store $0 $1 $2 $3
cd ../../../..