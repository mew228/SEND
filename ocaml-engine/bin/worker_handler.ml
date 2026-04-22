type handler_error = {
  code : string;
  message : string;
  details : string list;
}

let ( let* ) result f =
  match result with
  | Ok value -> f value
  | Error _ as error -> error

let protocol_error message details =
  Error { code = "protocol_error"; message; details }

let spec_decode_error details =
  Error
    {
      code = "spec_decode_failed";
      message = "Failed to decode node specs.";
      details;
    }

let graph_validation_error details =
  Error
    {
      code = "graph_validation_failed";
      message = "Graph validation failed.";
      details;
    }

let execution_error details =
  Error
    {
      code = "execution_failed";
      message = "Graph execution failed.";
      details;
    }

let missing_executor_error details =
  Error
    {
      code = "missing_executor";
      message = "Graph execution failed because a node executor is missing.";
      details;
    }

let failure_response request error =
  Worker_protocol.Failure
    {
      code = error.code;
      message = error.message;
      command = Some request.Worker_protocol.command;
      details = error.details;
    }

let success_response request result =
  Worker_protocol.Success
    {
      command = request.Worker_protocol.command;
      result;
    }

let get_assoc_field fields field_name =
  match List.assoc_opt field_name fields with
  | Some (`Assoc value) -> Ok value
  | Some _ -> protocol_error ("Field `" ^ field_name ^ "` must be an object.") []
  | None -> protocol_error ("Missing object field `" ^ field_name ^ "`.") []

let get_list_field fields field_name =
  match List.assoc_opt field_name fields with
  | Some (`List value) -> Ok value
  | Some _ -> protocol_error ("Field `" ^ field_name ^ "` must be an array.") []
  | None -> protocol_error ("Missing array field `" ^ field_name ^ "`.") []

let get_string_field fields field_name =
  match List.assoc_opt field_name fields with
  | Some (`String value) -> Ok value
  | Some _ -> protocol_error ("Field `" ^ field_name ^ "` must be a string.") []
  | None -> protocol_error ("Missing string field `" ^ field_name ^ "`.") []

let get_int_field fields field_name =
  match List.assoc_opt field_name fields with
  | Some (`Int value) -> Ok value
  | Some (`Intlit value) -> (
      match int_of_string_opt value with
      | Some value -> Ok value
      | None -> protocol_error ("Field `" ^ field_name ^ "` must be an integer.") [])
  | Some _ -> protocol_error ("Field `" ^ field_name ^ "` must be an integer.") []
  | None -> protocol_error ("Missing integer field `" ^ field_name ^ "`.") []

let get_bool_field fields field_name =
  match List.assoc_opt field_name fields with
  | Some (`Bool value) -> Ok value
  | Some _ -> protocol_error ("Field `" ^ field_name ^ "` must be a bool.") []
  | None -> protocol_error ("Missing bool field `" ^ field_name ^ "`.") []

let get_float_field fields field_name =
  match List.assoc_opt field_name fields with
  | Some (`Float value) -> Ok value
  | Some (`Int value) -> Ok (float_of_int value)
  | Some (`Intlit value) -> (
      match float_of_string_opt value with
      | Some value -> Ok value
      | None -> protocol_error ("Field `" ^ field_name ^ "` must be a number.") [])
  | Some _ -> protocol_error ("Field `" ^ field_name ^ "` must be a number.") []
  | None -> protocol_error ("Missing number field `" ^ field_name ^ "`.") []

let decode_runtime_value field_name = function
  | `Int value -> Ok (Node.Number_value (float_of_int value))
  | `Intlit value -> (
      match float_of_string_opt value with
      | Some value -> Ok (Node.Number_value value)
      | None -> protocol_error ("Field `" ^ field_name ^ "` must be a JSON scalar.") [])
  | `Float value -> Ok (Node.Number_value value)
  | `Bool value -> Ok (Node.Bool_value value)
  | `String value -> Ok (Node.String_value value)
  | _ -> protocol_error ("Field `" ^ field_name ^ "` must be a JSON scalar.") []

let decode_data_field = function
  | `Assoc fields ->
      let* name = get_string_field fields "name" in
      let* raw_value =
        match List.assoc_opt "value" fields with
        | Some value -> Ok value
        | None -> protocol_error "Missing field `value` in node data field." []
      in
      let* value = decode_runtime_value "value" raw_value in
      Ok (Node.make_data_field ~name ~value)
  | _ -> protocol_error "Node data field entries must be objects." []

let decode_graph_node = function
  | `Assoc fields ->
      let* id = get_string_field fields "id" in
      let* node_type = get_string_field fields "nodeType" in
      let* data_fields =
        match List.assoc_opt "dataFields" fields with
        | None -> Ok []
        | Some (`List values) ->
            let rec loop acc = function
              | [] -> Ok (List.rev acc)
              | value :: remaining ->
                  let* decoded = decode_data_field value in
                  loop (decoded :: acc) remaining
            in
            loop [] values
        | Some _ -> protocol_error "Field `dataFields` must be an array." []
      in
      Ok (Node.make ~id:(Node_id.of_string id) ~node_type ~data_fields ())
  | _ -> protocol_error "Graph nodes must be objects." []

let decode_edge = function
  | `Assoc fields ->
      let* source_node = get_string_field fields "sourceNode" in
      let* source_port = get_int_field fields "sourcePort" in
      let* target_node = get_string_field fields "targetNode" in
      let* target_port = get_int_field fields "targetPort" in
      Ok
        (Node.make_edge
           ~source:(Node.make_port_ref ~node_id:(Node_id.of_string source_node) ~port_index:source_port)
           ~target:(Node.make_port_ref ~node_id:(Node_id.of_string target_node) ~port_index:target_port))
  | _ -> protocol_error "Graph edges must be objects." []

let decode_node_specs values =
  let rec loop specs errors = function
    | [] ->
        if errors = [] then
          Ok (List.rev specs)
        else
          spec_decode_error (List.rev errors)
    | value :: remaining -> (
        match Node_spec_decoder.decode_json value with
        | Ok spec -> loop (spec :: specs) errors remaining
        | Error error -> loop specs (Spec_error.to_string error :: errors) remaining)
  in
  loop [] [] values

let decode_graph_payload payload =
  match payload with
  | None -> protocol_error "Worker request payload is required." []
  | Some (`Assoc fields) ->
      let* graph_fields = get_assoc_field fields "graph" in
      let* node_spec_values = get_list_field fields "nodeSpecs" in
      let* node_specs = decode_node_specs node_spec_values in
      let* node_values = get_list_field graph_fields "nodes" in
      let* edge_values = get_list_field graph_fields "edges" in
      let rec decode_list decoder acc = function
        | [] -> Ok (List.rev acc)
        | value :: remaining ->
            let* decoded = decoder value in
            decode_list decoder (decoded :: acc) remaining
      in
      let* nodes = decode_list decode_graph_node [] node_values in
      let* edges = decode_list decode_edge [] edge_values in
      Ok (Graph.create ~node_specs ~nodes ~edges ())
  | Some _ -> protocol_error "Worker request payload must be an object." []

let decode_simulation_payload payload =
  match payload with
  | None -> protocol_error "Worker request payload is required." []
  | Some (`Assoc fields) ->
      let* graph = decode_graph_payload payload in
      let* simulation_fields = get_assoc_field fields "simulation" in
      let* start_date = get_string_field simulation_fields "startDate" in
      let* end_date = get_string_field simulation_fields "endDate" in
      let* initial_cash = get_float_field simulation_fields "initialCash" in
      let* include_trace = get_bool_field simulation_fields "includeTrace" in
      Ok (graph, start_date, end_date, initial_cash, include_trace)
  | Some _ -> protocol_error "Worker request payload must be an object." []

let value_to_json = function
  | Node.Number_value value ->
      if Float.is_integer value then `Int (int_of_float value) else `Float value
  | Node.Bool_value value -> `Bool value
  | Node.String_value value -> `String value

let is_hidden_constant_node = function
  | "const_number"
  | "const_bool"
  | "const_string" -> true
  | _ -> false

let collect_output_results graph =
  Graph.nodes graph
  |> List.filter_map (fun (node : Node.t) ->
         if is_hidden_constant_node node.node_type then
           None
         else
           match Graph.find_node_spec graph node.node_type with
           | None -> None
           | Some spec ->
               let outputs =
                 spec.output_ports
                 |> List.filter_map (fun (port : Node.port_spec) ->
                        let port_ref = Node.make_port_ref ~node_id:node.id ~port_index:port.index in
                        match Graph.port_value graph port_ref with
                        | None -> None
                        | Some value -> Some (port.name, value_to_json value))
               in
               if outputs = [] then None else Some (Node_id.to_string node.id, `Assoc outputs))
  |> fun entries -> `Assoc entries

let validate_graph_result graph =
  match Graph.validate graph with
  | Ok () -> Ok (`Assoc [ ("valid", `Bool true) ])
  | Error errors -> graph_validation_error (List.map Graph_error.to_string errors)

let classify_engine_error (error : Engine_error.t) =
  match error with
  | Engine_error.Graph_validation_failed errors ->
      (`Graph_validation_failed, List.map Graph_error.to_string errors)
  | Engine_error.Missing_executor _ ->
      (`Missing_executor, [ Engine_error.to_string error ])
  | _ ->
      (`Execution_failed, [ Engine_error.to_string error ])

let execute_graph_result graph =
  match Engine.execute ~simulation:None ~graph ~registry:Primitive_registry.registry with
  | Ok executed_graph -> Ok (collect_output_results executed_graph)
  | Error errors ->
      let kinds, details =
        List.fold_left
          (fun (kinds, details) error ->
            let kind, error_details = classify_engine_error error in
            (kind :: kinds, List.rev_append error_details details))
          ([], [])
          errors
      in
      let details = List.rev details in
      if List.exists (( = ) `Graph_validation_failed) kinds then
        graph_validation_error details
      else if List.exists (( = ) `Missing_executor) kinds then
        missing_executor_error details
      else
        execution_error details

let handle_validate_graph request =
  match decode_graph_payload request.Worker_protocol.payload with
  | Ok graph -> (
      match validate_graph_result graph with
      | Ok result -> success_response request result
      | Error error -> failure_response request error)
  | Error error -> failure_response request error

let handle_execute_graph request =
  match decode_graph_payload request.Worker_protocol.payload with
  | Ok graph -> (
      match execute_graph_result graph with
      | Ok result -> success_response request result
      | Error error -> failure_response request error)
  | Error error -> failure_response request error

let execution_failure_from_details details =
  match execution_error details with
  | Error error -> error
  | Ok _ -> assert false

let handle_simulate_graph request =
  match decode_simulation_payload request.Worker_protocol.payload with
  | Ok (graph, start_date, end_date, initial_cash, include_trace) -> (
      match
        Simulation.simulate
          ~graph
          ~registry:Primitive_registry.registry
          ~start_date
          ~end_date
          ~initial_cash
          ~include_trace
      with
      | Ok result -> success_response request result
      | Error details -> failure_response request (execution_failure_from_details details))
  | Error error -> failure_response request error

let handle_request request =
  match request.Worker_protocol.command with
  | Worker_protocol.Validate_graph -> handle_validate_graph request
  | Worker_protocol.Execute_graph -> handle_execute_graph request
  | Worker_protocol.Simulate_graph -> handle_simulate_graph request
