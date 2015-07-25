create table "user" ("userID" VARCHAR NOT NULL PRIMARY KEY,"firstName" VARCHAR,"lastName" VARCHAR,"fullName" VARCHAR,"email" VARCHAR,"avatarURL" VARCHAR);

create table "logininfo" ("id" BIGSERIAL NOT NULL PRIMARY KEY,"providerID" VARCHAR NOT NULL,"providerKey" VARCHAR NOT NULL);

create table "userlogininfo" ("userID" VARCHAR NOT NULL,"loginInfoId" BIGINT NOT NULL);

create table "passwordinfo" ("hasher" VARCHAR NOT NULL,"password" VARCHAR NOT NULL,"salt" VARCHAR,"loginInfoId" BIGINT NOT NULL);

create table "oauth1info" ("id" BIGSERIAL NOT NULL PRIMARY KEY,"token" VARCHAR NOT NULL,"secret" VARCHAR NOT NULL,"loginInfoId" BIGINT NOT NULL);

create table "oauth2info" ("id" BIGSERIAL NOT NULL PRIMARY KEY,"accesstoken" VARCHAR NOT NULL,"tokentype" VARCHAR,"expiresin" INTEGER,"refreshtoken" VARCHAR,"logininfoid" BIGINT NOT NULL);

create table "transport_modality" ("transport_modality_id" VARCHAR NOT NULL PRIMARY KEY,"transport_modality_name" VARCHAR NOT NULL);

create table "region" ("region_id" VARCHAR NOT NULL PRIMARY KEY,"region_name" VARCHAR DEFAULT '' NOT NULL,"region_description" VARCHAR DEFAULT '' NOT NULL,"region_thumbnail" VARCHAR,"super_region_id" VARCHAR DEFAULT null);

alter table "region" add constraint "fk_region_super_region" foreign key("super_region_id") references "region"("region_id") on update CASCADE on delete CASCADE;

create table "city" ("city_id" VARCHAR NOT NULL PRIMARY KEY,"city_name" VARCHAR NOT NULL,"city_description" VARCHAR DEFAULT '' NOT NULL,"region_id" VARCHAR NOT NULL);

alter table "city" add constraint "fk_city_region" foreign key("region_id") references "region"("region_id") on update CASCADE on delete CASCADE;

create table "trip" ("trip_id" VARCHAR NOT NULL PRIMARY KEY,"trip_name" VARCHAR DEFAULT '' NOT NULL,"trip_is_public" BOOLEAN DEFAULT false NOT NULL,"user_id" VARCHAR NOT NULL,"region_id" VARCHAR NOT NULL);

alter table "trip" add constraint "fk_trip_region" foreign key("trip_id") references "region"("region_id") on update CASCADE on delete RESTRICT;

alter table "trip" add constraint "fk_trip_user" foreign key("user_id") references "user"("userID") on update CASCADE on delete RESTRICT;

create table "trip_day" ("trip_id" VARCHAR NOT NULL,"day_number" INTEGER NOT NULL,"day_label" VARCHAR);

alter table "trip_day" add constraint "fk_day" primary key("trip_id","day_number");

alter table "trip_day" add constraint "fk_trip_days_trip" foreign key("trip_id") references "trip"("trip_id") on update CASCADE on delete CASCADE;

create table "activity" ("trip_id" VARCHAR NOT NULL,"day_number" INTEGER NOT NULL,"activity_order" INTEGER NOT NULL,"length_hours" INTEGER NOT NULL);

alter table "activity" add constraint "pk_activity" primary key("trip_id","day_number","activity_order");

alter table "activity" add constraint "fk_activity_trip_day" foreign key("trip_id","day_number") references "trip_day"("trip_id","day_number") on update CASCADE on delete CASCADE;

create table "visit_activity" ("trip_id" VARCHAR NOT NULL,"day_number" INTEGER NOT NULL,"activity_order" INTEGER NOT NULL,"visit_poi_id" VARCHAR NOT NULL,"visit_description" VARCHAR DEFAULT '' NOT NULL);

alter table "visit_activity" add constraint "pk_visit_activity" primary key("trip_id","day_number","activity_order");

alter table "visit_activity" add constraint "fk_visit_activity_day" foreign key("trip_id","day_number","activity_order") references "activity"("trip_id","day_number","activity_order") on update CASCADE on delete CASCADE;

alter table "visit_activity" add constraint "fk_visit_activity_poi" foreign key("visit_poi_id") references "poi"("poi_id") on update CASCADE on delete CASCADE;

create table "transport_activity" ("trip_id" VARCHAR NOT NULL,"day_number" INTEGER NOT NULL,"activity_order" INTEGER NOT NULL,"from_city_id" VARCHAR NOT NULL,"to_city_id" VARCHAR NOT NULL,"transport_modality_id" VARCHAR NOT NULL,"transport_description" VARCHAR DEFAULT '' NOT NULL);

alter table "transport_activity" add constraint "pk_transport_activity" primary key("trip_id","day_number","activity_order");

alter table "transport_activity" add constraint "fk_transport_activity_day" foreign key("trip_id","day_number","activity_order") references "activity"("trip_id","day_number","activity_order") on update CASCADE on delete CASCADE;

alter table "transport_activity" add constraint "fk_transport_activity_from_city" foreign key("from_city_id") references "city"("city_id") on update CASCADE on delete CASCADE;

alter table "transport_activity" add constraint "fk_transport_activity_modality" foreign key("transport_modality_id") references "transport_modality"("transport_modality_id") on update CASCADE on delete CASCADE;

alter table "transport_activity" add constraint "fk_transport_activity_to_city" foreign key("to_city_id") references "city"("city_id") on update CASCADE on delete CASCADE;

create table "poi" ("poi_id" VARCHAR NOT NULL PRIMARY KEY,"poi_name" VARCHAR NOT NULL,"poi_description" VARCHAR DEFAULT '' NOT NULL,"city_id" VARCHAR NOT NULL);

alter table "poi" add constraint "fk_poi_city" foreign key("city_id") references "city"("city_id") on update CASCADE on delete CASCADE;
