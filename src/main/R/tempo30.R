options(java.parameters = "-Xmx8000m")
library(tidyverse)
library(dplyr)
library(patchwork)
library(networkD3)
library(sf) #=> geography
library(matsim)
library(stringr)
library("xlsx")
library(ggalluvial)
library(reshape2)

## person in gladbeck
#persons<- read_csv2("/Users/gregorr/Documents/work/respos/git/matsim-gladbeck/scenarios/output/output_gladbeck-v1.0-10pct/008.output_persons.csv.gz", col_types = cols(person = col_double(), income = col_double()))
persons <- read_delim("/Users/gregorr/Documents/work/respos/git/matsim-gladbeck/scenarios/output/output_gladbeck-v1.0-10pct/008.output_persons.csv.gz", delim = ";")
relevantPersons <- read_csv2("/Users/gregorr/Desktop/Test/GlaMoBi/Analysis/relevant-persons.csv",  col_types = cols(`person-id`= col_double()))
relevantPersons <- rename(relevantPersons, person = `person-id`)
persons <- left_join(relevantPersons, persons, by="person")

####### data tyding
baseCaseTrips <- matsim::readTripsTable("/Users/gregorr/Desktop/Test/GlaMoBi/workShop/basCaseContinued/gladbeck-v1.0.output_trips.csv.gz")
policyCaseTrips <- matsim::readTripsTable("/Users/gregorr/Desktop/Test/GlaMoBi/workShop/tempo30V1.1/gladbeck-v1.0.output_trips.csv.gz")
baseCaseTrips <- filter(baseCaseTrips, person %in% persons$person)
policyCaseTrips <- filter(policyCaseTrips, person %in% persons$person)


baseCaseTrips <- baseCaseTrips %>%
  mutate(end_activity_type = sapply(strsplit(end_activity_type,"_"),"[[",1)) 

policyCaseTrips <- policyCaseTrips %>%
  mutate(end_activity_type = sapply(strsplit(end_activity_type,"_"),"[[",1)) 

### modal Shift

nrOfTripsBaseCase <- baseCaseTrips %>% group_by(main_mode) %>%
  count(name = "nrOfTripsBaseCase") 

nrOfTripsPolicyCase <- policyCaseTrips %>% group_by(main_mode) %>%
  count(name = "nrOfTripsPolicyCase") 
nrOfTrips <- left_join(nrOfTripsBaseCase, nrOfTripsPolicyCase, by = "main_mode")
nrOfTrips <-nrOfTrips %>% mutate(nrOfTripsPolicyCase = nrOfTripsPolicyCase*10)
nrOfTrips <-nrOfTrips %>% mutate(nrOfTripsBaseCase = nrOfTripsBaseCase*10)

nrOfTrips <- melt(nrOfTrips)
names(nrOfTrips)[3] <- "nrOfTripsPolicyCase"

modalShift <-ggplot(nrOfTrips, aes(x = main_mode, y= nrOfTripsPolicyCase, fill = variable)) +
  geom_bar(stat="identity", width=.5, position = "dodge") +
  ylab("Anzahl an Fahrten") +
  scale_y_continuous(name=" Anzahl an Fahrten", labels = scales::comma_format(big.mark  = ".")) +
  theme_minimal() +
  scale_fill_discrete(name = NULL, labels = c("Base Case", "Tempo 30")) +
  ggtitle("Anzahl Fahrten je Verkehrsmittel") +
  xlab("Verkehrsmodus")
modalShift

########################### trav time avg

tripDataPolicyCaseNoLongTrips <- filter(policyCaseTrips, traveled_distance <20000)
tripDataBaseCaseNoLongTrips<- filter(baseCaseTrips, traveled_distance <20000)

baseCaseTripsAverageTravTime <-tripDataBaseCaseNoLongTrips %>%
  group_by(main_mode) %>%
  summarise(mean = mean(trav_time))

policyCaseTripsAverageTravTime  <- tripDataPolicyCaseNoLongTrips %>%
  group_by(main_mode) %>%
  summarise(mean = mean(trav_time))

baseCaseAverageTimePlot <- ggplot(data = baseCaseTripsAverageTravTime, aes(main_mode, mean, fill = main_mode)) +
  geom_col() +
  ylab("durchschnittliche Reisezeit in s") +
  theme_minimal() +
  ylim(0,2000) +
  geom_text(aes(label= round(mean,2)), nsmall=2, vjust=-0.5) +
  xlab("Verkehrsmittel") +
  labs(title = "Durchschnittliche Reisezeit: Basisfall") +
  theme(legend.position = "none")

policyCaseAverageTimePlot <- ggplot(data = policyCaseTripsAverageTravTime, aes(main_mode, mean, fill = main_mode)) +
  geom_col() +
  ylab("durchschnittliche Reisezeit in s") +
  theme_minimal() +
  ylim(0,2000) +
  geom_text(aes(label= round(mean,2)), nsmall=2, vjust=-0.5) +
  xlab("Verkehrsmittel") +
  labs(title = "Durchschnittliche Reisezeit: Tempo 30") +
  scale_fill_discrete(name = "Verkehrsmittel")

baseCaseAverageTimePlot + policyCaseAverageTimePlot

#### tripDistance
tripDataPolicyCaseNoLongTrips <- filter(policyCaseTrips, traveled_distance <20000)
tripDataBaseCaseNoLongTrips<- filter(baseCaseTrips, traveled_distance <20000)


baseCaseTripsAverageTravDistance <-tripDataBaseCaseNoLongTrips %>%
  group_by(main_mode) %>%
  dplyr::summarise(mean = mean(traveled_distance))

policyCaseTripsAverageTravDistance  <- tripDataPolicyCaseNoLongTrips %>%
  group_by(main_mode) %>%
  summarise(mean = mean(traveled_distance))

baseCaseAverageTravDistancePlot <- ggplot(data = baseCaseTripsAverageTravDistance, aes(main_mode, mean, fill = main_mode)) +
  geom_col() +
  ylab("durchschnittliche Tripdistanz in m") +
  theme_minimal() +
  xlab("Verkehrsmittel") +
  geom_text(aes(label= round(mean,2)), nsmall=2, vjust=-0.5) +
  labs(title = "Durchschnittliche Tripdistanz: Basisfall") +
  theme(legend.position = "none")

policyCaseAverageTravDistancePlot <- ggplot(data = policyCaseTripsAverageTravDistance, aes(main_mode, mean, fill = main_mode)) +
  geom_col() +
  ylab("durchschnittliche Tripdistanz in m") +
  theme_minimal() +
  xlab("Verkehrsmittel") +
  labs(title = "Durchschnittliche Tripdistanz: Tempo 30") +
  geom_text(aes(label= round(mean,2)), nsmall=2, vjust=-0.5) +
  scale_fill_discrete(name = "Verkehrsmittel")

baseCaseAverageTravDistancePlot + policyCaseAverageTravDistancePlot


baseCasePtTrips <- filter(baseCaseTrips, main_mode == "pt")

test <- ggplot(baseCasePtTrips, aes(traveled_distance)) +
  geom_histogram(bindwith=1000)
test

average <- median(baseCasePtTrips$traveled_distance) 


############# plotting trip Distance
tripDataPolicyCaseNoLongTrips <- filter(policyCaseTrips, traveled_distance <4000000)
tripDataBaseCaseNoLongTrips<- filter(baseCaseTrips, traveled_distance <4000000)
group.colors <- c(walk = "#333BFF", bike = "#ff9100", car ="#fc0505", pt = "#32fc05", ride = "#a8a8a8")
distanceTripDataBaseCase <- ggplot(data=tripDataBaseCaseNoLongTrips, aes(x=traveled_distance, fill=main_mode)) +
  geom_area(stat = "bin")+
  theme_classic() +
  labs(title = "Agenten im Base Case Continued", x="Trip Distanz [m]", y="Anzahl") +
  scale_fill_manual(values=group.colors)

distancePolicyCaseDistrubition <- ggplot(data=tripDataPolicyCaseNoLongTrips, aes(x=traveled_distance, fill=main_mode)) +
  geom_area(stat = "bin")+
  theme_classic()+
  labs(title = "Kostenloser ÖPNV", x="Trip Distanz [m]", y="Anzahl") +
  scale_fill_manual(values=group.colors)
distanceTripDataBaseCase +distancePolicyCaseDistrubition
