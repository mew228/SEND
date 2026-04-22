val decode_json : Yojson.Safe.t -> (Node.node_spec, Spec_error.t) result
val decode_string : string -> (Node.node_spec, Spec_error.t) result
val decode_file : string -> (Node.node_spec, Spec_error.t) result
