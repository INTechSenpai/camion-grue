#!/bin/sh
dir=$(pwd)

cd /D/Documents/Git/eurobotruck/config/core
git pull
mvn install -DskipTests

cd /D/Documents/Git/eurobotruck/log
git pull
mvn install -DskipTests

cd /D/Documents/Git/eurobotruck/dependency-injector
git pull
mvn install -DskipTests

cd /D/Documents/Git/eurobotruck/graphic-toolbox/core
git pull
mvn install -DskipTests

cd /D/Documents/Git/eurobotruck/The-Kraken-Pathfinding/core
git pull
mvn install -DskipTests

cd $dir
git pull
./update.sh
