type t

val empty : t
val add : Executor.t -> t -> t
val find : string -> t -> Executor.t option
val of_list : Executor.t list -> t
