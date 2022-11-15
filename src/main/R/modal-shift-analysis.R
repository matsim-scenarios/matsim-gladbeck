library(tidyverse)
library(dplyr)
library(networkD3)
library(sf) #=> geography

#############################################################
## INPUT DEFINITION ##

# shapeFile for filtering. if you modify the shp you should also modify the areaName!
shpFileName <- "../svn/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-shp/berlin.shp"
areaName <- "berlin"

shpFileName <- "../svn/public-svn/matsim/scenarios/countries/de/berlin/projects/pave/shp-files/berlin-planungsraum-hundekopf/berlin-hundekopf-based-on-planungsraum.shp"
areaName <- "hundekopf"

# determines whether spatial filtering is to be conducted on the base case trips.
#if TRUE, trips from base case are differentiated as quellverkehr, zielverkehr, binnenverkehr and the combination of all. takes some time as spatial computations are needed.
#output csv are dumped out such that 
#if FALSE, these csvs can be read in and one saves the computation time.
filterBase = FALSE


#############################################################
### SCRIPT START ##

#read raw data
policy_rawData <- read.csv2(paste(policy_scenario, "/output-", policy_runID, "/", policy_runID, ".output_trips.csv", sep = ""))
base_rawData <- read.csv2(paste("output-", base_runID, "/", base_runID, ".output_trips.csv", sep = ""))  

##########################################################################################################################################
## read data ##
if(filterBase){

  ## this is how to filter the base case data for trips within the shape file. You can instead read in the filtered data, once generated for the first time
  # filter for trips with origin AND destination within shape. Select columns that are necessary

    shpFile <- st_read(shpFileName)
        
    (baseGeoms <-base_rawData
      %>% mutate(wkt = paste("MULTIPOINT((", start_x, " ", start_y, "),(", end_x, " ", end_y,"))", sep =""))
      %>% st_as_sf(wkt = "wkt", crs= st_crs(shpFile))
    )
    
    (base <- baseGeoms
      %>% filter(st_intersects(shpFile, ., sparse=FALSE))
    )
    base <- as_tibble(base) %>% select(person,trip_id, main_mode)
    
    (base_binnenV <- baseGeoms
    %>% filter(st_contains(shpFile, ., sparse=FALSE))
    )
    base_binnenV <- as_tibble(base_binnenV) %>% select(person,trip_id, main_mode)
    
    (base_startInside <- base_rawData
      %>% mutate(wkt = paste("MULTIPOINT((", start_x, " ", start_y, "))", sep =""))
      %>% st_as_sf(wkt = "wkt", crs= st_crs(shpFile))
      %>% filter(st_contains(shpFile, ., sparse=FALSE))
    )
    base_startInside <- as_tibble(base_startInside) %>% select(person, trip_id, main_mode)
    
    (base_endInside <- base_rawData
      %>% mutate(wkt = paste("MULTIPOINT((", end_x, " ", end_y, "))", sep =""))
      %>% st_as_sf(wkt = "wkt", crs= st_crs(shpFile))
      %>% filter(st_contains(shpFile, ., sparse=FALSE))
    )
    
    base_endInside <- as_tibble(base_endInside) %>% select(person, trip_id, main_mode)
    
    base_quellV <- anti_join(base_startInside, base_binnenV, by = "trip_id")
    base_zielV <- anti_join(base_endInside, base_binnenV, by = "trip_id")
    
    write.csv2(base, file=paste("output-", base_runID, "/", base_runID, ".output_trips-", areaName, ".csv", sep = ""), row.names = FALSE)
    write.csv2(base_binnenV, file=paste("output-", base_runID, "/", base_runID, ".output_trips-", areaName, "-binnenVerkehr.csv", sep = ""), row.names = FALSE)
    write.csv2(base_quellV, file=paste("output-", base_runID, "/", base_runID, ".output_trips-", areaName, "-quellVerkehr.csv", sep = ""), row.names = FALSE)
    write.csv2(base_zielV, file=paste("output-", base_runID, "/", base_runID, ".output_trips-", areaName, "-zielVerkehr.csv", sep = ""), row.names = FALSE)
    
} else {
    base <- read.csv2(paste("output-", base_runID, "/", base_runID, ".output_trips-", areaName, ".csv", sep = ""), sep = ";", dec = ",")
    base_binnenV <- read.csv2(paste("output-", base_runID, "/", base_runID, ".output_trips-", areaName, "-binnenVerkehr.csv", sep = ""), sep = ";", dec = ",")
    base_quellV <- read.csv2(paste("output-", base_runID, "/", base_runID, ".output_trips-", areaName, "-quellVerkehr.csv", sep = ""), sep = ";", dec = ",")
    base_zielV <- read.csv2(paste("output-", base_runID, "/", base_runID, ".output_trips-", areaName, "-zielVerkehr.csv", sep = ""), sep = ";", dec = ",") 
}

policy <- policy_rawData %>%
  select(person, trip_id, main_mode)

##########################################################################################################################################
## process data ##

binnen_car <- base_binnenV %>% filter(main_mode == "car")
quell_car <- base_quellV %>% filter(main_mode == "car")
ziel_car <- base_zielV %>% filter(main_mode == "car")
base_car <- base %>% filter(main_mode == "car")

## quellVerkehr ##

(
  # join tables. base tibble is spatially filtered, so we do a left join in order to take only the rows within base tibble
  quellVerkehr <- left_join(base_quellV, policy, by = "trip_id", keep = TRUE, suffix = c(".base", ".policy"))   %>%  rename(person = person.base, trip_id = trip_id.base, from = main_mode.base, to = main_mode.policy)
  %>% select(person, trip_id, from, to)
  %>% filter(from == "car")

)
# get nr of car trips
nrTrips <- summarise(quellVerkehr, n = n())[1,1]

#dump out data
outputFile <- paste(policy_scenario, "/output-", policy_runID, "/", policy_runID, ".trips-", areaName, "-quellVerkehr-modalShift-to-", base_runID, ".csv", sep = "")
print(paste("writing ouput to", outputFile))
write.csv2(quellVerkehr, file = outputFile, sep = ';', dec = '../../../../../../../tubCloud/Masterarbeit/Analysen/RAnalysen/Skripte', row.names = FALSE)

quell_intermodal <- quellVerkehr %>% 
  filter(to == "car_w_drt_used") %>% 
  nrow(.)

# group
quellVerkehr_grouped <- quellVerkehr %>% 
  group_by(from, to) %>% 
  filter(!is.na(to), !is.na(from)) %>% 
  tally() %>% 
  mutate(rel = (n/nrTrips))
# print
quellVerkehr_grouped

## zielVerkehr ##

(
  # join tables. base tibble is spatially filtered, so we do a left join in order to take only the rows within base tibble
  zielVerkehr <- left_join(base_zielV, policy, by = "trip_id", keep = TRUE, suffix = c(".base", ".policy"))
  %>% rename(person = person.base, trip_id = trip_id.base, from = main_mode.base, to = main_mode.policy)
  %>% select(person, trip_id, from, to)
  %>% filter(from == "car")
  
)
# print summary
nrTrips <- summarise(zielVerkehr, n = n())[1,1]

#dump out data
outputFile <- paste(policy_scenario, "/output-", policy_runID, "/", policy_runID, ".trips-", areaName, "-zielVerkehr-modalShift-to-", base_runID, ".csv", sep = "")
print(paste("writing ouput to", outputFile))
write.csv2(zielVerkehr, file = outputFile, sep = ';', dec = '../../../../../../../tubCloud/Masterarbeit/Analysen/RAnalysen/Skripte', row.names = FALSE)

ziel_intermodal <- zielVerkehr %>% 
  filter(to == "car_w_drt_used") %>% 
  nrow(.)

# group
zielVerkehr_grouped <- zielVerkehr %>% 
  group_by(from, to) %>% 
  filter(!is.na(to), !is.na(from)) %>% 
  tally() %>% 
  mutate(rel = (n/nrTrips))
# print
zielVerkehr_grouped


## binnen ##

(
  # join tables. base tibble is spatially filtered, so we do a left join in order to take only the rows within base tibble
  binnenVerkehr <- left_join(base_binnenV, policy, by = "trip_id", keep = TRUE, suffix = c(".base", ".policy"))
  %>% rename(person = person.base, trip_id = trip_id.base, from = main_mode.base, to = main_mode.policy)
  %>% select(person, trip_id, from, to)
  %>% filter(from == "car")
  
)
# print summary
nrTrips <- summarise(binnenVerkehr, n = n())[1,1]

#dump out data
outputFile <- paste(policy_scenario, "/output-", policy_runID, "/", policy_runID, ".trips-", areaName, "-binnenVerkehr-modalShift-to-", base_runID, ".csv", sep = "")
print(paste("writing ouput to", outputFile))
write.csv2(binnenVerkehr, file = outputFile, sep = ';', dec = '../../../../../../../tubCloud/Masterarbeit/Analysen/RAnalysen/Skripte', row.names = FALSE)

binnen_intermodal <- binnenVerkehr %>% 
  filter(to == "car_w_drt_used") %>% 
  nrow(.)

# group
binnenVerkehr_grouped <- binnenVerkehr %>% 
  group_by(from, to) %>% 
  filter(!is.na(to), !is.na(from)) %>% 
  tally() %>% 
  mutate(rel = (n/nrTrips))
# print
binnenVerkehr_grouped




##### count total intermodal trips
tripsOutside <- anti_join(base_rawData, base , by = "trip_id")

intermodalTripsOutside <- left_join(tripsOutside, policy, by = "trip_id", keep = TRUE, suffix = c(".base", ".policy"))   %>%  
  rename(person = person.base, trip_id = trip_id.base, from = main_mode.base, to = main_mode.policy) %>% 
  filter(to == "car_w_drt_used")

totalIntermodalTrips <- policy %>% 
  filter(main_mode == "car_w_drt_used")

rel <- nrow(totalIntermodalTrips) / nrow(policy)
print(paste("total nr of intermodal trips = ", nrow(totalIntermodalTrips), ". share of car-adopters=", rel))
print(paste("nr of intermodal trips outside = ", nrow(intermodalTripsOutside)))
print(paste("nr of intermodal trips quellVerkehr = ", quell_intermodal, ". share of car-adopters=", quell_intermodal/nrow(quellVerkehr)))
print(paste("nr of intermodal trips zielVerkehr = ", ziel_intermodal, ". share of car-adopters=", ziel_intermodal/nrow(zielVerkehr)))
print(paste("nr of intermodal trips binnenVerkehr = ", binnen_intermodal, ". share of car-adopters=", binnen_intermodal/nrow(binnenVerkehr)))

