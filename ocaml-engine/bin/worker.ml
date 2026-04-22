let process_line line =
  match Worker_protocol.decode_request line with
  | Ok request ->
      Printf.eprintf
        "worker received command: %s\n%!"
        (Worker_protocol.command_to_string request.command);
      Worker_handler.handle_request request |> Worker_protocol.encode_response
  | Error protocol_error ->
      Printf.eprintf
        "worker rejected request: %s\n%!"
        (Worker_protocol.error_message protocol_error);
      Worker_protocol.response_of_protocol_error protocol_error
      |> Worker_protocol.encode_response

let rec loop () =
  match input_line stdin with
  | line ->
      output_string stdout (process_line line);
      output_char stdout '\n';
      flush stdout;
      loop ()
  | exception End_of_file -> ()

let () = loop ()
