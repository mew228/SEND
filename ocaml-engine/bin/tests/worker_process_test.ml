let assert_true condition message =
  if not condition then failwith message

let read_all_lines channel =
  let rec loop lines =
    match input_line channel with
    | line -> loop (line :: lines)
    | exception End_of_file -> List.rev lines
  in
  loop []

let non_empty_lines lines =
  List.filter (fun line -> String.trim line <> "") lines

let shell_escape value =
  String.concat "\"\"" (String.split_on_char '"' value)

let () =
  if Array.length Sys.argv < 2 then failwith "worker executable path missing";
  let worker_path = Sys.argv.(1) in
  let input_path = Filename.temp_file "worker-input" ".jsonl" in
  let stdout_path = Filename.temp_file "worker-stdout" ".log" in
  let stderr_path = Filename.temp_file "worker-stderr" ".log" in
  let input_channel = open_out input_path in
  output_string
    input_channel
    {|{"command":"validate_graph","payload":{"graph":{"nodes":[],"edges":[]},"nodeSpecs":[]}}|};
  output_char input_channel '\n';
  output_string input_channel "not-json\n";
  close_out input_channel;
  let command =
    Printf.sprintf
      "cmd /c \"\"%s\" < \"%s\" > \"%s\" 2> \"%s\"\""
      (shell_escape worker_path)
      (shell_escape input_path)
      (shell_escape stdout_path)
      (shell_escape stderr_path)
  in
  let exit_code = Sys.command command in
  assert_true (exit_code = 0) "worker process command failed";
  let stdout_channel = open_in stdout_path in
  let stderr_channel = open_in stderr_path in
  let stdout_lines = read_all_lines stdout_channel |> non_empty_lines in
  let stderr_lines = read_all_lines stderr_channel |> non_empty_lines in
  close_in stdout_channel;
  close_in stderr_channel;
  let first_stdout, second_stdout =
    match stdout_lines with
    | first :: second :: _ -> (first, second)
    | _ -> failwith ("expected two worker stdout lines, got: " ^ String.concat " | " stdout_lines)
  in
  let first_json = Yojson.Safe.from_string first_stdout in
  let second_json = Yojson.Safe.from_string second_stdout in
  assert_true
    (Yojson.Safe.Util.member "status" first_json = `String "ok")
    "expected ok response for valid request";
  assert_true
    (Yojson.Safe.Util.member "command" first_json = `String "validate_graph")
    "expected validate_graph command in success response";
  assert_true
    (Yojson.Safe.Util.member "status" second_json = `String "error")
    "expected error response for malformed request";
  assert_true
    (Yojson.Safe.Util.member "code" second_json = `String "invalid_json")
    "expected invalid_json code";
  assert_true (stderr_lines <> []) "expected worker logs on stderr";
  assert_true
    (List.exists
       (fun line ->
         String.starts_with ~prefix:"worker received command:"
           (String.trim line)
         || String.starts_with ~prefix:"worker rejected request:"
              (String.trim line))
       stderr_lines)
    "expected command logs on stderr only"
