options(java.parameters = "-Xmx8000m")
library(tidyverse)
library(dplyr)
library(patchwork)
library(networkD3)
library(sf) #=> geography
library(matsim)
library(stringr)
library("xlsx")

## person in gladbeck
#persons<- read_csv2("/Users/gregorr/Documents/work/respos/git/matsim-gladbeck/scenarios/output/output_gladbeck-v1.0-10pct/008.output_persons.csv.gz", col_types = cols(person = col_double(), income = col_double()))
persons <- read_delim("/Users/gregorr/Desktop/Test/GlaMoBi/workShop/basCaseContinued/gladbeck-v1.0.output_persons.csv.gz", delim = ";")
relevantPersons <- read_csv2("/Users/gregorr/Desktop/Test/GlaMoBi/Analysis/relevant-persons.csv",  col_types = cols(`person-id`= col_double()))
relevantPersons <- rename(relevantPersons, person = `person-id`)
persons <- left_join(relevantPersons, persons, by="person")

####### data tyding
baseCaseTrips <- matsim::readTripsTable("/Users/gregorr/Desktop/Test/GlaMoBi/workShop/basCaseContinued/gladbeck-v1.0.output_trips.csv.gz")
policyCaseTrips <- matsim::readTripsTable("/Users/gregorr/Desktop/Test/GlaMoBi/workShop/ptFlat/gladbeck-v1.0.output_trips.csv.gz")
baseCaseTrips <- filter(baseCaseTrips, person %in% persons$person)
policyCaseTrips <- filter(policyCaseTrips, person %in% persons$person)

policyCaseTrips <-rename(policyCaseTrips, Verkehrsmittel = main_mode)
baseCaseTrips <-rename(baseCaseTrips, Verkehrsmittel = main_mode)


baseCaseTrips <- baseCaseTrips %>%
  mutate(end_activity_type = sapply(strsplit(end_activity_type,"_"),"[[",1)) 

policyCaseTrips <- policyCaseTrips %>%
  mutate(end_activity_type = sapply(strsplit(end_activity_type,"_"),"[[",1)) 


######## modal shift

### modal Shift

baseCaseTrips$Verkehrsmittel[grep(paste0(unite.columns, collapse = "|"), baseCaseTrips$Verkehrsmittel)] <- united.name
policyCaseTrips$Verkehrsmittel[grep(paste0(unite.columns, collapse = "|"), policyCaseTrips$Verkehrsmittel)] <- united.name

baseCaseTrips  = baseCaseTrips %>% mutate(type = "Basisfall")
policyCaseTrips  = policyCaseTrips %>% mutate(type = "kostenloser ÖPNV")

total_trips = rbind(baseCaseTrips,policyCaseTrips)

plt = ggplot(total_trips, aes(x =Verkehrsmittel,fill = factor(type)))+
  geom_bar(position = position_dodge())+
  theme_minimal() +
  ylab("Anzahl Fahrten") +
  xlab("Verkehrsmittel") +
  scale_fill_discrete(name = "") +
  labs(title = "Anzahl Fahrten je Verkehrsmittel")
plt

########################### trav time avg
baseCaseTripsAverageTravTime <-baseCaseTrips %>%
  group_by(Verkehrsmittel) %>%
  summarise(mean = mean(trav_time))

policyCaseTripsAverageTravTime  <- policyCaseTrips %>%
  group_by(Verkehrsmittel) %>%
  summarise(mean = mean(trav_time))

baseCaseAverageTimePlot <- ggplot(data = baseCaseTripsAverageTravTime, aes(Verkehrsmittel, mean, fill = Verkehrsmittel)) +
  geom_col() +
  ylab("durchschnittliche Reisezeit in s") +
  theme_minimal() +
  ylim(0,5000) +
  ggtitle("Basisfall")

policyCaseAverageTimePlot <- ggplot(data = policyCaseTripsAverageTravTime, aes(Verkehrsmittel, mean, fill = Verkehrsmittel)) +
  geom_col() +
  ylab("durchschnittliche Reisezeit in s") +
  theme_minimal() +
  ylim(0,5000) +
  ggtitle("Kostenloser ÖPNV")
  
baseCaseAverageTimePlot + policyCaseAverageTimePlot

############# plotting trip Distance
tripDataPolicyCaseNoLongTrips <- filter(policyCaseTrips, traveled_distance <20000)
tripDataBaseCaseNoLongTrips<- filter(baseCaseTrips, traveled_distance <20000)
group.colors <- c(walk = "#333BFF", bike = "#ff9100", car ="#fc0505", pt = "#32fc05", ride = "#a8a8a8")
distanceTripDataBaseCase <- ggplot(data=tripDataBaseCaseNoLongTrips, aes(x=traveled_distance, fill=Verkehrsmittel)) +
  geom_area(stat = "bin")+
  theme_classic() +
  labs(title = "Agenten im Base Case Continued", x="Trip Distanz [m]", y="Anzahl") +
  scale_fill_manual(values=group.colors)

distancePolicyCaseDistrubition <- ggplot(data=tripDataPolicyCaseNoLongTrips, aes(x=traveled_distance, fill=Verkehrsmittel)) +
  geom_area(stat = "bin")+
  theme_classic()+
  labs(title = "Kostenloser ÖPNV", x="Trip Distanz [m]", y="Anzahl") +
  scale_fill_manual(values=group.colors)
distanceTripDataBaseCase +distancePolicyCaseDistrubition


####################### trip purpose 
baseCasePtTrips <- filter(baseCaseTrips, Verkehrsmittel=="pt")
policyCasePtTrips <- filter(policyCaseTrips, Verkehrsmittel =="pt")

baseCasePtTripsPurposePlot <- ggplot(baseCasePtTrips, aes(x = end_activity_type)) + 
  geom_bar(aes(y = (..count../sum(..count..)))) +
  ylab("Anteil [%]") +
  xlab("Aktivitäten") +
  scale_y_continuous(labels = scales::percent) +
  theme_minimal()

policyCasePtTripsPlot <- ggplot(policyCasePtTrips, aes (x =  end_activity_type)) + 
  geom_bar(aes(y = (..count../sum(..count..)))) +
  ylab("Anteil [%]") +
  xlab("Aktivitäten") +
  scale_y_continuous(labels = scales::percent) +
  theme_minimal()

baseCasePtTripsPurposePlot + policyCasePtTripsPlot

###################################### social attributes
nrOfPtTripsPerPersonBaseCase <- baseCasePtTrips %>% 
   group_by(person) %>% summarise(nrOfPtTrips = n()) 

nrOfPtTripsPerPolicyCase <- policyCasePtTrips %>% 
  group_by(person) %>% summarise(nrOfPtTrips = n()) 

nrOfPtTripsPerPersonBaseCase <- left_join(nrOfPtTripsPerPersonBaseCase, persons, by = "person")
nrOfPtTripsPerPolicyCase <- left_join(nrOfPtTripsPerPolicyCase, persons, by = "person")

########income
incomeDistributionPtUsersBaseCase <- ggplot(nrOfPtTripsPerPersonBaseCase, aes(income)) +
  geom_area(stat = "bin", fill = "lightgrey") +
  theme_minimal() + 
  ylab("Häufigkeit") +
  xlab("Einkommen") +
  ylim(0, 200)
  
incomeDistributionPtUsersPolicyCase <- ggplot(nrOfPtTripsPerPolicyCase, aes(income)) +
  geom_area(stat = "bin", fill = "lightgrey") +
  theme_minimal() + 
  ylab("Häufigkeit") +
  xlab("Einkommen") +
  ylim(0,200)

incomeDistributionPtUsersBaseCase + incomeDistributionPtUsersPolicyCase