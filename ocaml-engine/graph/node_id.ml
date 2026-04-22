type t = string

let compare = String.compare
let of_string value = value
let to_string value = value

module Map = Stdlib.Map.Make (struct
  type nonrec t = t

  let compare = compare
end)

module Set = Stdlib.Set.Make (struct
  type nonrec t = t

  let compare = compare
end)
