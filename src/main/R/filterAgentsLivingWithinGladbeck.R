library(tidyverse)
library(dplyr)
library(networkD3)
library(sf) #=> geography


shpFileName <- "../../shared-svn/projects/GlaMoBi/data/shp-files/Gladbeck.shp"
base_rawData <- read.csv2("output_metropole-ruhr-v1.0-10pct/metropole-ruhr-v1.0-10pct.output_trips.csv.gz")

filterdDataForHomeActivity <- filter(base_rawData, str_detect(base_rawData$start_activity_type, "home"))

##filter for Agents livin within Gladbeck
shpFile <- st_read(shpFileName)
agentsLivingWithinGladbeck<-(filterdDataForHomeActivity
  %>% mutate(wkt = paste("MULTIPOINT((", start_x, " ", start_y, "))", sep =""))
  %>% st_as_sf(wkt = "wkt", crs= st_crs(shpFile))
  %>% filter(st_contains(shpFile, ., sparse=FALSE)))
agentsLivingWithinGladbeck<-select(agentsLivingWithinGladbeck, person, start_activity_type, start_x, start_y, wkt)

#write out data
write.csv2(agentsLivingWithinGladbeck, file = "agentsLivingWithinGladbeck.csv")