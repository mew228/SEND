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

let command_to_string = function
  | Validate_graph -> "validate_graph"
  | Execute_graph -> "execute_graph"
  | Simulate_graph -> "simulate_graph"

let error_code = function
  | Invalid_json _ -> "invalid_json"
  | Missing_command -> "missing_command"
  | Invalid_command_type -> "invalid_command_type"
  | Unknown_command _ -> "unknown_command"

let error_message = function
  | Invalid_json message -> "invalid JSON: " ^ message
  | Missing_command -> "missing required string field `command`"
  | Invalid_command_type -> "field `command` must be a string"
  | Unknown_command command -> "unknown command: " ^ command

let command_of_string = function
  | "validate_graph" -> Ok Validate_graph
  | "execute_graph" -> Ok Execute_graph
  | "simulate_graph" -> Ok Simulate_graph
  | command -> Error (Unknown_command command)

let decode_request line =
  match Yojson.Safe.from_string line with
  | exception Yojson.Json_error message -> Error (Invalid_json message)
  | json -> (
      match json with
      | `Assoc fields ->
          let payload = List.assoc_opt "payload" fields in
          begin
            match List.assoc_opt "command" fields with
            | None -> Error Missing_command
            | Some (`String command) -> (
                match command_of_string command with
                | Ok command -> Ok { command; payload }
                | Error _ as error -> error)
            | Some _ -> Error Invalid_command_type
          end
      | _ -> Error Missing_command)

let response_of_protocol_error error =
  Failure
    {
      code = error_code error;
      message = error_message error;
      command = None;
      details = [];
    }

let encode_response response =
  let json =
    match response with
    | Success { command; result } ->
        `Assoc
          [
            ("status", `String "ok");
            ("command", `String (command_to_string command));
            ("result", result);
          ]
    | Failure { code; message; command; details } ->
        let fields =
          [
            Some ("status", `String "error");
            Some ("code", `String code);
            Some ("message", `String message);
            Option.map (fun command -> ("command", `String (command_to_string command))) command;
            Some ("details", `List (List.map (fun detail -> `String detail) details));
          ]
          |> List.filter_map Fun.id
        in
        `Assoc fields
  in
  Yojson.Safe.to_string json
