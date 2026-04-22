val simulate :
  graph:Graph.t ->
  registry:Executor_registry.t ->
  start_date:string ->
  end_date:string ->
  initial_cash:float ->
  include_trace:bool ->
  (Yojson.Safe.t, string list) result
