val execute :
  simulation:Executor.simulation_services option ->
  graph:Graph.t ->
  registry:Executor_registry.t ->
  (Graph.t, Engine_error.t list) result

val execute_node :
  simulation:Executor.simulation_services option ->
  Graph.t ->
  Executor_registry.t ->
  Node.t ->
  (Graph.t, Engine_error.t list) result
