
JAR := matsim-gladbeck-*.jar
V := v1.0
CRS := EPSG:25832

$(JAR):
	mvn package

POPULATION := ../shared-svn/projects/matsim-metropole-ruhr/metropole-ruhr-v1.0/input/metropole-ruhr-v1.4-25pct.plans.xml.gz
NETWORK := ../public-svn/matsim/scenarios/countries/de/metropole-ruhr/metropole-ruhr-v1.0/input/metropole-ruhr-v1.4.network_resolutionHigh-with-pt.xml.gz
SHAPE := scenarios/study-area-utm.shp
export JAVA_TOOL_OPTIONS := -Xmx50G -Xms50G

scenarios/input/gladbeck-$V-25pct.plans.xml.gz:

	java -jar $(JAR) prepare scenario-cutout\
		--input $(POPULATION)\
		--network $(NETWORK)\
		--input-crs $(CRS)\
		--shp $(SHP)\
		--output-network scenarios/input/gladbeck-$V-network.xml.gz\
		--output-population $@\
		--use-router

	java -jar $(JAR) prepare xy-to-links --network scenarios/input/gladbeck-$V-network.xml.gz\
		 --input $@\
		 --output $@\
		 --car-only

	java -jar $(JAR) prepare fix-subtour-modes --input $@ --output $@

	java -jar $(JAR) prepare downsample-population $@\
		 --sample-size 0.25\
		 --samples 0.1 0.01

scenarios/input/gladbeck-$V-homes.csv: scenarios/input/gladbeck-$V-25pct.plans.xml.gz
	java -jar $(JAR) prepare extract-home-coordinates $< --csv $@

prepare: scenarios/input/gladbeck-$V-25pct.plans.xml.gz scenarios/input/gladbeck-$V-homes.csv
	echo "Done"