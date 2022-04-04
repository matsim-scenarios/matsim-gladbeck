library(tidyverse)
library(dplyr)
library(ggplot2)
library(patchwork)

setwd("/output_metropole-ruhr-v1.0-10pct/")

personData <-  read_delim(file ="metropole-ruhr-v1.0-10pct.output_persons.csv.gz", 
                          delim = ";")

ggplot(data= personData, aes(personData$`microm:modeled:age`)) +
  geom_area(stat="bin")








