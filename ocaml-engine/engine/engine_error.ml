type t =
  | Graph_validation_failed of Graph_error.t list
  | Missing_executor of {
      node_id : Node_id.t;
      executor_key : string;
    }
  | Missing_input_value of {
      node_id : Node_id.t;
      port_index : int;
    }
  | Executor_failed of {
      node_id : Node_id.t;
      executor_key : string;
      message : string;
    }
  | Invalid_executor_output_port of {
      node_id : Node_id.t;
      port_index : int;
    }
  | Invalid_executor_output_type of {
      node_id : Node_id.t;
      port_index : int;
      expected_kind : Node.value_kind;
      actual : Node.value;
    }
  | Graph_state_write_failed of Graph_error.t list

let to_string = function
  | Graph_validation_failed errors ->
      "Graph validation failed: " ^ String.concat " | " (List.map Graph_error.to_string errors)
  | Missing_executor { node_id; executor_key } ->
      "Missing executor `" ^ executor_key ^ "` for node " ^ Node_id.to_string node_id
  | Missing_input_value { node_id; port_index } ->
      "Missing input value for node "
      ^ Node_id.to_string node_id
      ^ " port "
      ^ string_of_int port_index
  | Executor_failed { node_id; executor_key; message } ->
      "Executor `" ^ executor_key ^ "` failed for node " ^ Node_id.to_string node_id ^ ": " ^ message
  | Invalid_executor_output_port { node_id; port_index } ->
      "Executor returned invalid output port "
      ^ string_of_int port_index
      ^ " for node "
      ^ Node_id.to_string node_id
  | Invalid_executor_output_type { node_id; port_index; expected_kind; actual } ->
      "Executor returned invalid output type for node "
      ^ Node_id.to_string node_id
      ^ " port "
      ^ string_of_int port_index
      ^ ": expected "
      ^ Node.value_kind_label expected_kind
      ^ " but got "
      ^ Node.value_label actual
  | Graph_state_write_failed errors ->
      "Graph state write failed: " ^ String.concat " | " (List.map Graph_error.to_string errors)
