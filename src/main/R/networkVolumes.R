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



####analyse the link volumes changes scenario wide

##total volumes
totalVolumes <- read.csv2("/Users/gregorr/Documents/work/respos/public-svn/matsim/scenarios/countries/de/gladbeck/glamobi/projects/glamobi/v2.0/base-case/gladbeck-v2.0.linkPaxVolumesPerNetworkModePerHour.csv.gz")

##total car volumes
totalCarVolumes <- filter(totalVolumes, networkMode == "car")

##
volumSum <- sum(totalCarVolumes$vehicles)

##linkVol changes in gladgeck
linksInGladbeck <- read_tsv("/Users/gregorr/Documents/work/respos/shared-svn/projects/GlaMoBi/networkLinksWithinGladbeck.tsv")
carVolGladbeck <- filter(totalCarVolumes, link %in% linksInGladbeck$linkId)

totalCarVolumesGladbeck <- sum(carVolGladbeck$vehicles)







