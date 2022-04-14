library(tidyverse)
library(dplyr)
library(patchwork)

#reading in data
agentsLivingWithinGladbeck <- read.csv2("/output_metropole-ruhr-v1.0-10pct/agentsLivingWithinGladbeck.csv")

#complete person data
completePersonData <- read_delim(file ="/work/respos/git/matsim-gladbeck/scenarios/output/output_metropole-ruhr-v1.0-10pct/metropole-ruhr-v1.0-10pct.output_persons.csv.gz",
                                 delim = ";",col_types = cols(income= col_double()))

incomeClasses <- completePersonData %>% group_by(income) %>%
  summarise(income_nr= n())

gladbeckAgentsData<-left_join(agentsLivingWithinGladbeck,completePersonData, by = 'person')

completePersonData <- filter(completePersonData, income < 75000)
gladbeckAgentsData <- filter(gladbeckAgentsData, income < 75000)

p <- ggplot(gladbeckAgentsData, aes(`microm:modeled:sex`, income))
p + geom_violin(draw_quantiles = c(0.25, 0.5, 0.75),colour = "#3366FF") +
  labs(x="Geschlecht", y="Einkommen",title = "Verteilung des Einkommens der Agenten differenziert nach Geschlecht") +
  theme_classic()


pAll <- ggplot(completePersonData, aes(`microm:modeled:sex`, income))
pAll + geom_violin(draw_quantiles = c(0.25, 0.5, 0.75),colour = "#3366FF") +
  labs(x="Geschlecht", y="Einkommen",title = "Verteilung des Einkommens der Agenten differenziert nach Geschlecht") +
  theme_classic()

p <- ggplot(gladbeckAgentsData, aes(`microm:modeled:age`, income))
p + geom_smooth(method = lm)  +
  labs(x="Alter", y="Einkommen",title = "Verteilung des Einkommens der Agenten differenziert nach Alter") +
  theme_classic()

pAll <- ggplot(gladbeckAgentsData, aes(`microm:modeled:age`, income))
pAll + geom_smooth(method = lm)
