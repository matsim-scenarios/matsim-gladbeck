library(tidyverse)
library(dplyr)
library(ggplot2)
library(patchwork)

setwd("/output_metropole-ruhr-v1.0-10pct")

tripBaseCase <-  read_delim(file ="metropole-ruhr-v1.0-10pct.output_trips.csv.gz",
                            delim = ";")

tripBaseCaseActivityChains <- group_by(tripBaseCase,start_activity_type ,end_activity_type)
tripBaseCaseActivityChains$start_activity_type<-gsub("home_.*","home", tripBaseCaseActivityChains$start_activity_type)
tripBaseCaseActivityChains$start_activity_type<-gsub("leisure_.*","leisure", tripBaseCaseActivityChains$start_activity_type)
tripBaseCaseActivityChains$start_activity_type<-gsub("work_.*","work", tripBaseCaseActivityChains$start_activity_type)
tripBaseCaseActivityChains$start_activity_type<-gsub("other_.*","other", tripBaseCaseActivityChains$start_activity_type)
tripBaseCaseActivityChains$start_activity_type<-gsub("shop_daily.*","shop_daily", tripBaseCaseActivityChains$start_activity_type)
tripBaseCaseActivityChains$start_activity_type<-gsub("educ_higher_.*","educ_higher", tripBaseCaseActivityChains$start_activity_type)
tripBaseCaseActivityChains$start_activity_type<-gsub("educ_kiga_.*","educ_kiga", tripBaseCaseActivityChains$start_activity_type)
tripBaseCaseActivityChains$start_activity_type<-gsub("business_.*","business", tripBaseCaseActivityChains$start_activity_type)
tripBaseCaseActivityChains$start_activity_type<-gsub("educ_primary_.*","educ_primary", tripBaseCaseActivityChains$start_activity_type)
tripBaseCaseActivityChains$start_activity_type<-gsub("educ_other_.*","educ_other", tripBaseCaseActivityChains$start_activity_type)
tripBaseCaseActivityChains$start_activity_type<-gsub("educ_tertiary_.*","educ_tertiary", tripBaseCaseActivityChains$start_activity_type)
tripBaseCaseActivityChains$start_activity_type<-gsub("educ_secondary_.*","educ_secondary", tripBaseCaseActivityChains$start_activity_type)
tripBaseCaseActivityChains$start_activity_type<-gsub("errands_.*","errands", tripBaseCaseActivityChains$start_activity_type)
tripBaseCaseActivityChains$start_activity_type<-gsub("shop_other.*","shop_other", tripBaseCaseActivityChains$start_activity_type)
tripBaseCaseActivityChains$start_activity_type<-gsub("visit.*","visit", tripBaseCaseActivityChains$start_activity_type)

tripBaseCaseActivityChains$end_activity_type<-gsub("home_.*","home", tripBaseCaseActivityChains$end_activity_type)
tripBaseCaseActivityChains$end_activity_type<-gsub("leisure_.*","leisure", tripBaseCaseActivityChains$end_activity_type)
tripBaseCaseActivityChains$end_activity_type<-gsub("work_.*","work", tripBaseCaseActivityChains$end_activity_type)
tripBaseCaseActivityChains$end_activity_type<-gsub("other_.*","other", tripBaseCaseActivityChains$end_activity_type)
tripBaseCaseActivityChains$end_activity_type<-gsub("shop_daily.*","shop_daily", tripBaseCaseActivityChains$end_activity_type)
tripBaseCaseActivityChains$end_activity_type<-gsub("business_.*","business", tripBaseCaseActivityChains$end_activity_type)
tripBaseCaseActivityChains$end_activity_type<-gsub("educ_higher_.*","educ_higher", tripBaseCaseActivityChains$end_activity_type)
tripBaseCaseActivityChains$end_activity_type<-gsub("educ_kiga_.*","educ_kiga", tripBaseCaseActivityChains$end_activity_type)
tripBaseCaseActivityChains$end_activity_type<-gsub("educ_primary_.*","educ_primary", tripBaseCaseActivityChains$end_activity_type)
tripBaseCaseActivityChains$end_activity_type<-gsub("educ_other_.*","educ_other", tripBaseCaseActivityChains$end_activity_type)
tripBaseCaseActivityChains$end_activity_type<-gsub("educ_tertiary_.*","educ_tertiary", tripBaseCaseActivityChains$end_activity_type)
tripBaseCaseActivityChains$end_activity_type<-gsub("educ_secondary_.*","educ_secondary", tripBaseCaseActivityChains$end_activity_type)
tripBaseCaseActivityChains$end_activity_type<-gsub("errands_.*","errands", tripBaseCaseActivityChains$end_activity_type)
tripBaseCaseActivityChains$end_activity_type<-gsub("shop_other.*","shop_other", tripBaseCaseActivityChains$end_activity_type)
tripBaseCaseActivityChains$end_activity_type<-gsub("visit.*","visit", tripBaseCaseActivityChains$end_activity_type)

tripBaseCaseActivityChains <- group_by(tripBaseCaseActivityChains,start_activity_type ,end_activity_type) %>%
  count()

tripBaseCaseActivityChains <- tripBaseCaseActivityChains %>%
  add_column(Start_and_End_Activity = "Value")

tripBaseCaseActivityChains$Start_and_End_Activity <- paste(tripBaseCaseActivityChains$start_activity_type, tripBaseCaseActivityChains$end_activity_type)

ggplot(tripBaseCaseActivityChains) +
  geom_bar(aes(x=Start_and_End_Activity))

write.csv(tripBaseCase, "D:/Gregor/Uni/TUCloud/Masterarbeit/MATSim/BaseCase/activityEndTest.csv")
