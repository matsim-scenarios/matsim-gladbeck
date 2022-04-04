library(tidyverse)
library(dplyr)
library(networkD3)
library(sf) #=> geography
library(tibble)

setwd("output_metropole-ruhr-v1.0-10pct")

#reading in data
personsLivingWithinGladbeck <- read_csv2("agentsLivingWithinGladbeck.csv")
personAttributes <-read_csv2("metropole-ruhr-v1.0-10pct.output_persons.csv.gz")

#only analyses Agents livning in Gladbeck
personsLivingWithinGladbeck <- left_join(personsLivingWithinGladbeck, personAttributes, by= "person")

#age
personsLivingWithinGladbeck <- personsLivingWithinGladbeck %>%
  mutate(age_group = cut(`microm:modeled:age`,
                         breaks = c(-1, 3, 6, 10, 18, 25, 27, 50, 65,80, Inf),
                         labels = c("0-3", "3-6", "6-10", "10-18", "18-25", "25-27", "27-50", "50-65", "65-80","80+")))

numberOfAgentsInEachAgeGroup <- personsLivingWithinGladbeck %>% 
  group_by(age_group) %>%
  summarise(n())

# real Data from Gladbeck
# https://eservice2.gkd-re.de/bsointer140/DokumentServlet?dokumentenname=140l10365.pdf
ageDataFromGladbeck <- read_csv2("/../../shared-svn/projects/GlaMoBi/data/sozio-demographischen_Daten/Altersverteilung_Gladbeck.csv")
ageDataFromGladbeckAndAgents<-left_join(numberOfAgentsInEachAgeGroup, ageDataFromGladbeck, by="age_group")
ageDataFromGladbeckAndAgents <- dplyr::rename(ageDataFromGladbeckAndAgents, x=`n()`)


df1 <- data.frame(ageDataFromGladbeckAndAgents$`x`, ageDataFromGladbeckAndAgents$`scaled`, ageDataFromGladbeck$age_group)

df1$ageDataFromGladbeckAndAgents.x

df2 <- tidyr::pivot_longer(df1, cols=c('ageDataFromGladbeckAndAgents.x', 'ageDataFromGladbeckAndAgents.scaled'), names_to='variable', 
                           values_to="value")

ggplot(df2, aes(x=value, y= ageDataFromGladbeck.age_group, fill=variable)) +
  geom_bar(stat='identity', position='dodge') +
  ggtitle("Altersvariable: Modell vs. Realität") +
  labs(x="Anzahl", y="Altersgruppen", fill = "Legende:") +
  scale_y_discrete(limits = c("0-3","3-6","6-10","10-18","18-25","25-27","27-50","50-65","65-80","80+")) +
  scale_fill_discrete(labels = c("Realität", "Anzahl im Modell")) +
  theme_minimal()
  

#pt and car availability
# only pt available
onlyPtAvailable <- filter(personsLivingWithinGladbeck, sim_ptAbo == "full" & sim_carAvailability=="never")
# only car available
onlyCarAvailable <- filter(personsLivingWithinGladbeck, sim_ptAbo == "none" & sim_carAvailability=="always")
#both
bothAvailable <- filter(personsLivingWithinGladbeck, sim_ptAbo == "full" & sim_carAvailability=="always")



