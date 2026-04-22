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

val to_string : t -> string
