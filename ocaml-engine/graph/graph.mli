type t

val create :
  ?port_values:(Node.port_ref * Node.value) list ->
  node_specs:Node.node_spec list ->
  nodes:Node.t list ->
  edges:Node.edge list ->
  unit ->
  t

val node_specs : t -> Node.node_spec list
val nodes : t -> Node.t list
val edges : t -> Node.edge list
val find_node : t -> Node_id.t -> Node.t option
val find_node_spec : t -> string -> Node.node_spec option
val expected_port_kind : t -> Node.port_ref -> Node.value_kind option
val validate : t -> (unit, Graph_error.t list) result
val port_value : t -> Node.port_ref -> Node.value option
val set_port_value : t -> Node.port_ref -> Node.value -> (unit, Graph_error.t list) result
val clear_port_values : t -> unit
val downstream : t -> Node.port_ref -> Node.port_ref list
val incoming_count : t -> Node_id.t -> int
val topological_sort : t -> (Node.t list, Graph_error.t list) result
