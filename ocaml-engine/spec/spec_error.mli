type t =
  | Missing_field of string
  | Invalid_field_type of string
  | Unknown_value_type of string
  | Unknown_port_arity of string
  | Invalid_json of string

val to_string : t -> string
