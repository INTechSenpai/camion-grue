#!/bin/sh
dir=$(pwd)

cd ~/config/core
git pull
mvn install -DskipTests

cd ~/dependency-injector
git pull
mvn install -DskipTests

cd ~/graphic-toolbox/core
git pull
mvn install -DskipTests

cd ~/The-Kraken-Pathfinding/core
git pull
mvn install -DskipTests

cd $dir
git pull
./update.sh
