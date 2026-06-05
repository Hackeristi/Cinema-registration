using System;
using System.Collections.Generic;

namespace Projekt1_Server.Models;

public partial class FilmShow
{
    public int FilmShowId { get; set; }

    public int MovieId { get; set; }

    public int ScreenId { get; set; }

    public DateTime ShowDatetime { get; set; }

    public virtual Movie? Movie { get; set; }

    public virtual ICollection<Reservation> Reservations { get; set; } = new List<Reservation>();

    public virtual Screen? Screen { get; set; }
}
