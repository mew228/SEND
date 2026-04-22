type t =
  | Duplicate_node_id of Node_id.t
  | Duplicate_node_type of string
  | Missing_node_spec of string
  | Missing_source_node of Node_id.t
  | Missing_target_node of Node_id.t
  | Invalid_source_port of Node.port_ref
  | Invalid_target_port of Node.port_ref
  | Incompatible_edge_types of {
      source : Node.port_ref;
      target : Node.port_ref;
      source_kind : Node.value_kind;
      target_kind : Node.value_kind;
    }
  | Unknown_data_field of {
      node_id : Node_id.t;
      field_name : string;
    }
  | Invalid_node_data_field of {
      node_id : Node_id.t;
      field_name : string;
      expected_kind : Node.value_kind;
      actual_value : Node.value;
    }
  | Invalid_port_value of {
      port : Node.port_ref;
      expected_kind : Node.value_kind;
      actual_value : Node.value;
    }
  | Invalid_multi_input_arity of {
      node_id : Node_id.t;
      port_index : int;
    }
  | Missing_multi_input_connection of {
      node_id : Node_id.t;
      port_index : int;
    }
  | Too_many_incoming_edges of {
      node_id : Node_id.t;
      port_index : int;
      actual_count : int;
    }
  | Cycle_detected of Node_id.t list

val to_string : t -> string
