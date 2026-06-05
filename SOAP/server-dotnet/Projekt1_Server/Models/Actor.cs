using System;
using System.Collections.Generic;

namespace Projekt1_Server.Models;

public partial class Actor
{
    public int ActorId { get; set; }

    public string Name { get; set; }

    public string Surmane { get; set; }

    public virtual ICollection<Movie> Movies { get; set; } = new List<Movie>();
}
