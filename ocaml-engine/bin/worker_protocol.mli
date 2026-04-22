type command =
  | Validate_graph
  | Execute_graph
  | Simulate_graph

type request = {
  command : command;
  payload : Yojson.Safe.t option;
}

type protocol_error =
  | Invalid_json of string
  | Missing_command
  | Invalid_command_type
  | Unknown_command of string

type response =
  | Success of {
      command : command;
      result : Yojson.Safe.t;
    }
  | Failure of {
      code : string;
      message : string;
      command : command option;
      details : string list;
    }

val command_to_string : command -> string
val error_code : protocol_error -> string
val error_message : protocol_error -> string
val decode_request : string -> (request, protocol_error) result
val response_of_protocol_error : protocol_error -> response
val encode_response : response -> string
