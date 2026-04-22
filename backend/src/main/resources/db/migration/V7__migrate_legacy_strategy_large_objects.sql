UPDATE user_strategies
SET graph_json = convert_from(lo_get(graph_json::oid), 'UTF8')
WHERE graph_json ~ '^[0-9]+$'
  AND EXISTS (
    SELECT 1
    FROM pg_largeobject_metadata
    WHERE oid = graph_json::oid
  );
