create extension fuzzystrmatch;

create extension unaccent;

drop index idx_feature_name_trigrams;

create or replace function unaccent(text)
	returns text
	immutable strict
	language "sql"
AS $$ 
select (coalesce(ts_lexize('unaccent',$1),ARRAY[$1]))[1];
$$;

CREATE OR REPLACE FUNCTION trigrams_array(word text)
        RETURNS text[]
        IMMUTABLE STRICT
        LANGUAGE "plpgsql"
AS $$
        DECLARE
                result text[];
		word2 text;
        BEGIN
		word2 := unaccent(word);
                FOR i IN 1 .. length(word2) - 2 LOOP
                        result := result || quote_literal(substr(lower(word2), i, 3));
                END LOOP;

                RETURN result;
        END;
$$;


CREATE OR REPLACE FUNCTION trigrams_vector(text)
        RETURNS tsvector
        IMMUTABLE STRICT
        LANGUAGE "sql"
AS $$
        SELECT array_to_string(trigrams_array($1), ' ')::tsvector;
$$;
CREATE OR REPLACE FUNCTION trigrams_query(text)
        RETURNS tsquery
        IMMUTABLE STRICT
        LANGUAGE "sql"
AS $$
        SELECT array_to_string(trigrams_array($1), ' & ')::tsquery;
$$;

create index idx_feature_name_trigrams on feature using gin(trigrams_vector(name));
