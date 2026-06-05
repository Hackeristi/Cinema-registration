using System;
using System.Collections.Generic;

namespace Projekt1_Server.Models;

public partial class Screen
{
    public int ScreenId { get; set; }

    public int Number { get; set; }

    public virtual ICollection<FilmShow> FilmShows { get; set; } = new List<FilmShow>();

    public virtual ICollection<Seat> Seats { get; set; } = new List<Seat>();
}
