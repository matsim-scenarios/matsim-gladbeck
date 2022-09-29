library(tidyverse)
library(dplyr)
library(patchwork)

#reading in data
agentsLivingWithinGladbeck <- read_csv2("/Users/gregorr/Desktop/Test/GlaMoBi/Data/person2Home.csv")
agentsLivingWithinGladbeck <- filter(personsLivingWithinGladbeck, area=="Gladbeck")


#complete person data
completePersonData <- read_delim(file ="/Users/gregorr/Documents/work/respos/runs-svn/rvr-ruhrgebiet/v1.2.1/036/036.output_persons.csv",
                                 delim = ";")

incomeClasses <- completePersonData %>% group_by(income) %>%
  summarise(income_nr= n())


##SZN delivered household income --> vsp convention 

completePersonData<-completePersonData %>% mutate(`MiD:hheink_gr2`=recode(`MiD:hheink_gr2`, `1`="500", `2`="900",  `3`="1500",
                                                      `4`="2000",  `5`="3000",  `6`="4000",  `7`="5000",  `8`="6000",  `9`="7000",  `10`="8000"))


incomeClassesAverage <- completePersonData %>% group_by(income) %>%
  summarise(income_nr= n())

incomeClassesSNZ <- completePersonData %>% group_by(`MiD:hheink_gr2`) %>%
  summarise(income_nr= n())

gladbeckAgentsData<-left_join(agentsLivingWithinGladbeck, completePersonData, by = 'person')


p <- ggplot(gladbeckAgentsData, aes(`microm:modeled:sex`, `MiD:hheink_gr2`))
p + geom_violin(draw_quantiles = c(0.25, 0.5, 0.75),colour = "#3366FF") +
  labs(x="Geschlecht", y="Einkommen",title = "Verteilung des Einkommens der Agenten differenziert nach Geschlecht") +
  theme_classic()

pAll <- ggplot(completePersonData, aes(`microm:modeled:sex`, `MiD:hheink_gr2`))
pAll + geom_violin(draw_quantiles = c(0.25, 0.5, 0.75),colour = "#3366FF") +
  labs(x="Geschlecht", y="Einkommen",title = "Verteilung des Einkommens der Agenten differenziert nach Geschlecht") +
  theme_classic()


p <- ggplot(gladbeckAgentsData, aes(`microm:modeled:sex`, `MiD:hheink_gr2`))
p + geom_count(colour = "#3366FF") +
  labs(x="Geschlecht", y="Einkommen",title = "Verteilung des Einkommens der Agenten differenziert nach Geschlecht")+
  theme_classic()

p <- ggplot(gladbeckAgentsData, aes(`MiD:hheink_gr2`, `microm:modeled:age`))
p + geom_violin()  +
  labs(x="Einkommen", y="Alter",title = "Verteilung des Einkommens der Agenten differenziert nach Alter") +
  theme_classic()

p <- ggplot(gladbeckAgentsData, aes(`microm:modeled:age`, `MiD:hheink_gr2`))
p + geom_smooth(method = lm)  +
  labs(x="Alter", y="Einkommen",title = "Verteilung des Einkommens der Agenten differenziert nach Alter") +
  theme_classic()

pAll <- ggplot(gladbeckAgentsData, aes(`microm:modeled:age`, `MiD:hheink_gr2`))
pAll + geom_smooth(method = lm)
