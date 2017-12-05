cd config/core
mvn install -DskipTests
cd ../..
cd dependency-injector
mvn install -DskipTests
cd ..
cd graphic-toolbox/core
mvn install -DskipTests
cd ../..
cd The-Kraken-Pathfinding/core
mvn install -DskipTests
cd ../..
cd eurobotruck/high_level
./update.sh
