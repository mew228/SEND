let source_ports_for_input graph node_id port_index =
  Graph.edges graph
  |> List.filter_map (fun (edge : Node.edge) ->
         if Node_id.compare edge.target.node_id node_id = 0 && edge.target.port_index = port_index then
           Some edge.source
         else
           None)

let find_node_spec_or_error graph node =
  match Graph.find_node_spec graph node.Node.node_type with
  | Some spec -> Ok spec
  | None -> Error [ Engine_error.Graph_validation_failed [ Graph_error.Missing_node_spec node.node_type ] ]

let collect_inputs graph node spec =
  let rec loop acc = function
    | [] -> Ok (List.rev acc)
    | (port : Node.port_spec) :: remaining ->
        begin
          match source_ports_for_input graph node.Node.id port.index with
          | [] ->
              Error
                [
                  Engine_error.Missing_input_value
                    {
                      node_id = node.id;
                      port_index = port.index;
                    };
                ]
          | source_ports -> (
              let rec collect_values collected = function
                | [] -> Ok (List.rev collected)
                | source_port :: remaining -> (
                    match Graph.port_value graph source_port with
                    | Some value -> collect_values (value :: collected) remaining
                    | None ->
                        Error
                          [
                            Engine_error.Missing_input_value
                              {
                                node_id = node.id;
                                port_index = port.index;
                              };
                          ])
              in
              match collect_values [] source_ports with
              | Ok values -> loop ((port.index, values) :: acc) remaining
              | Error _ as error -> error)
        end
  in
  loop [] spec.Node.input_ports

let validate_executor_outputs node spec outputs =
  List.filter_map
    (fun (port_index, value) ->
      match Node.find_output_port_spec spec port_index with
      | None ->
          Some
            (Engine_error.Invalid_executor_output_port
               {
                 node_id = node.Node.id;
                 port_index;
               })
      | Some port_spec when Node.kind_matches_value port_spec.value_kind value -> None
      | Some port_spec ->
          Some
            (Engine_error.Invalid_executor_output_type
               {
                 node_id = node.id;
                 port_index;
                 expected_kind = port_spec.value_kind;
                 actual = value;
               }))
    outputs

let write_outputs graph node outputs =
  let rec loop = function
    | [] -> Ok graph
    | (port_index, value) :: remaining ->
        let port_ref = Node.make_port_ref ~node_id:node.Node.id ~port_index in
        begin
          match Graph.set_port_value graph port_ref value with
          | Ok () -> loop remaining
          | Error errors -> Error [ Engine_error.Graph_state_write_failed errors ]
        end
  in
  loop outputs

let execute_node ~simulation graph registry node =
  match find_node_spec_or_error graph node with
  | Error _ as error -> error
  | Ok spec -> (
      match Executor_registry.find spec.Node.executor_key registry with
      | None ->
          Error
            [
              Engine_error.Missing_executor
                {
                  node_id = node.Node.id;
                  executor_key = spec.executor_key;
                };
            ]
      | Some executor -> (
          match collect_inputs graph node spec with
          | Error _ as error -> error
          | Ok inputs -> (
              match executor.Executor.run { node; node_spec = spec; inputs; simulation } with
              | Error error ->
                  Error
                    [
                      Engine_error.Executor_failed
                        {
                          node_id = node.id;
                          executor_key = executor.key;
                          message = Executor.run_error_to_string error;
                        };
                    ]
              | Ok outputs ->
                  match validate_executor_outputs node spec outputs with
                  | [] -> write_outputs graph node outputs
                  | errors -> Error errors)))

let execute ~simulation ~graph ~registry =
  match Graph.validate graph with
  | Error errors -> Error [ Engine_error.Graph_validation_failed errors ]
  | Ok () -> (
      match Graph.topological_sort graph with
      | Error errors -> Error [ Engine_error.Graph_validation_failed errors ]
      | Ok ordered_nodes ->
          let rec loop = function
            | [] -> Ok graph
            | node :: remaining -> (
                match execute_node ~simulation graph registry node with
                | Ok _ -> loop remaining
                | Error _ as error -> error)
          in
          loop ordered_nodes)
