type value =
  | Number_value of float
  | Bool_value of bool
  | String_value of string

type value_kind =
  | Number_kind
  | Bool_kind
  | String_kind
  | Any_kind

type port_arity =
  | One
  | Many

type port_spec = {
  index : int;
  name : string;
  arity : port_arity;
  value_kind : value_kind;
}

type data_field_spec = {
  name : string;
  value_kind : value_kind;
  required : bool;
  default_value : value option;
}

type node_spec = {
  node_type : string;
  set : string;
  display_name : string;
  description : string;
  input_ports : port_spec list;
  output_ports : port_spec list;
  data_fields : data_field_spec list;
  executor_key : string;
}

type data_field = {
  name : string;
  value : value;
}

type t = {
  id : Node_id.t;
  node_type : string;
  data_fields : data_field list;
}

type port_ref = {
  node_id : Node_id.t;
  port_index : int;
}

type edge = {
  source : port_ref;
  target : port_ref;
}

let value_label = function
  | Number_value _ -> "number"
  | Bool_value _ -> "bool"
  | String_value _ -> "string"

let value_kind_label = function
  | Number_kind -> "number"
  | Bool_kind -> "bool"
  | String_kind -> "string"
  | Any_kind -> "value"

let port_arity_label = function
  | One -> "ONE"
  | Many -> "MANY"

let same_value_kind left right =
  match (left, right) with
  | Number_value _, Number_value _ -> true
  | Bool_value _, Bool_value _ -> true
  | String_value _, String_value _ -> true
  | _ -> false

let value_kind_of_value = function
  | Number_value _ -> Number_kind
  | Bool_value _ -> Bool_kind
  | String_value _ -> String_kind

let kind_matches_value expected_kind value =
  match expected_kind with
  | Any_kind -> true
  | Number_kind -> value_kind_of_value value = Number_kind
  | Bool_kind -> value_kind_of_value value = Bool_kind
  | String_kind -> value_kind_of_value value = String_kind

let kinds_compatible left right =
  match (left, right) with
  | Any_kind, _
  | _, Any_kind -> true
  | Number_kind, Number_kind
  | Bool_kind, Bool_kind
  | String_kind, String_kind -> true
  | _ -> false

let equal_value left right =
  match (left, right) with
  | Number_value left_value, Number_value right_value -> left_value = right_value
  | Bool_value left_value, Bool_value right_value -> left_value = right_value
  | String_value left_value, String_value right_value -> left_value = right_value
  | _ -> false

let value_to_string = function
  | Number_value value -> string_of_float value
  | Bool_value value -> string_of_bool value
  | String_value value -> value

let number = Number_value 0.0
let bool = Bool_value false
let string = String_value ""
let number_kind = Number_kind
let bool_kind = Bool_kind
let string_kind = String_kind
let any_kind = Any_kind
let one = One
let many = Many
let normalize_number value = Number_value value
let make_port_spec ~index ~name ~arity ~value_kind : port_spec =
  { index; name; arity; value_kind }
let make_data_field_spec ~name ~value_kind ~required ~default_value : data_field_spec =
  { name; value_kind; required; default_value }

let make_spec
    ~node_type
    ~set
    ~display_name
    ~description
    ~input_ports
    ~output_ports
    ~data_fields
    ~executor_key
  =
  {
    node_type;
    set;
    display_name;
    description;
    input_ports;
    output_ports;
    data_fields;
    executor_key;
  }

let make_data_field ~name ~value : data_field = { name; value }
let make ?(data_fields = []) ~id ~node_type () = { id; node_type; data_fields }
let make_port_ref ~node_id ~port_index = { node_id; port_index }
let make_edge ~source ~target = { source; target }

let compare_port_ref left right =
  let by_node = Node_id.compare left.node_id right.node_id in
  if by_node <> 0 then by_node else Int.compare left.port_index right.port_index

module Port_ref_map = Stdlib.Map.Make (struct
  type nonrec t = port_ref

  let compare = compare_port_ref
end)

let find_input_port_spec spec port_index =
  List.find_opt (fun port -> port.index = port_index) spec.input_ports

let find_output_port_spec spec port_index =
  List.find_opt (fun port -> port.index = port_index) spec.output_ports

let find_data_field_spec (spec : node_spec) field_name =
  List.find_opt (fun (field : data_field_spec) -> field.name = field_name) spec.data_fields

let find_data_field (node : t) field_name =
  List.find_opt (fun field -> field.name = field_name) node.data_fields
