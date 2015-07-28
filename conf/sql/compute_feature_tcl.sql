begin;

drop table feature_tcl;

with recursive tcl(id,anc_id,depth,path,name_path,cycle) as (
  select bot_id,top_id, 1, ARRAY[bot_id],ARRAY[name], false from feature_hierarchy join feature on id = bot_id

  UNION ALL

  select tc.id, h.top_id, tc.depth + 1,
         tc.path || h.bot_id, tc.name_path || f.name, h.bot_id = ANY(path)
  from feature_hierarchy as h  join
       tcl tc on tc.anc_id = h.bot_id
       join feature f on f.id = h.bot_id
  where not cycle
)
SELECT tcl.id,anc_id,path || anc_id as path, name_path || name as name_path,depth 
into feature_tcl
FROM tcl join feature f on tcl.anc_id = f.id
ORDER BY tcl.id,anc_id;
 
alter table feature_tcl add primary key (path);
alter table feature_tcl add foreign key (id) references feature on delete cascade on update cascade;
alter table feature_tcl add foreign key (anc_id) references feature on delete cascade on update cascade;

grant all privileges on feature_tcl to tripspace;


commit;
