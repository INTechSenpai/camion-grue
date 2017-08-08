cd ../..

cd dependency-injector
git pull
rm *.jar
ant clean
ant
cd ..

cd config/core
git pull
rm *.jar
ant clean
ant
cd ../..

cd graphic-toolbox/core
git pull
cp ../../config/core/config.jar lib/
rm *.jar
ant clean
ant
cd ../..

cd The-Kraken-Pathfinding/core
git pull
cp ../../graphic-toolbox/core/graphic.jar lib/
cp ../../config/core/config.jar lib/
cp ../../dependency-injector/injector.jar lib/
rm *.jar
ant clean
ant
cd ../..

cd eurobotruck/high_level
git pull
cp ../../graphic-toolbox/core/graphic.jar lib/
cp ../../config/core/config.jar lib/
cp ../../dependency-injector/injector.jar lib/
cp ../../The-Kraken-Pathfinding/core/kraken.jar lib/
ant clean
ant
