module String_map = Stdlib.Map.Make (String)

type t = Executor.t String_map.t

let empty = String_map.empty
let add executor registry = String_map.add executor.Executor.key executor registry
let find key registry = String_map.find_opt key registry
let of_list executors = List.fold_left (fun registry executor -> add executor registry) empty executors
