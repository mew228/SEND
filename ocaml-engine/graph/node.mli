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

val value_label : value -> string
val value_kind_label : value_kind -> string
val port_arity_label : port_arity -> string
val same_value_kind : value -> value -> bool
val value_kind_of_value : value -> value_kind
val kind_matches_value : value_kind -> value -> bool
val kinds_compatible : value_kind -> value_kind -> bool
val equal_value : value -> value -> bool
val value_to_string : value -> string
val number : value
val bool : value
val string : value
val normalize_number : float -> value
val number_kind : value_kind
val bool_kind : value_kind
val string_kind : value_kind
val any_kind : value_kind
val one : port_arity
val many : port_arity
val make_port_spec : index:int -> name:string -> arity:port_arity -> value_kind:value_kind -> port_spec
val make_data_field_spec :
  name:string ->
  value_kind:value_kind ->
  required:bool ->
  default_value:value option ->
  data_field_spec

val make_spec :
  node_type:string ->
  set:string ->
  display_name:string ->
  description:string ->
  input_ports:port_spec list ->
  output_ports:port_spec list ->
  data_fields:data_field_spec list ->
  executor_key:string ->
  node_spec

val make_data_field : name:string -> value:value -> data_field
val make : ?data_fields:data_field list -> id:Node_id.t -> node_type:string -> unit -> t
val make_port_ref : node_id:Node_id.t -> port_index:int -> port_ref
val make_edge : source:port_ref -> target:port_ref -> edge
val compare_port_ref : port_ref -> port_ref -> int

module Port_ref_map : Map.S with type key = port_ref

val find_input_port_spec : node_spec -> int -> port_spec option
val find_output_port_spec : node_spec -> int -> port_spec option
val find_data_field_spec : node_spec -> string -> data_field_spec option
val find_data_field : t -> string -> data_field option
