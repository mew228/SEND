type t =
  | Missing_field of string
  | Invalid_field_type of string
  | Unknown_value_type of string
  | Unknown_port_arity of string
  | Invalid_json of string

let to_string = function
  | Missing_field field -> "Missing field: " ^ field
  | Invalid_field_type field -> "Invalid field type: " ^ field
  | Unknown_value_type value_type -> "Unknown value type: " ^ value_type
  | Unknown_port_arity arity -> "Unknown port arity: " ^ arity
  | Invalid_json message -> "Invalid JSON: " ^ message
