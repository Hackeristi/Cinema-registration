using System;
using System.Collections.Generic;

namespace Projekt1_Server.Models;

public partial class Seat
{
    public int SeatId { get; set; }

    public int ScreenId { get; set; }

    public int Number { get; set; }

    public int RowNum { get; set; }
    public bool IsTaken { get; set; }

    public virtual Screen? Screen { get; set; }

    public virtual ICollection<Reservation> Reservations { get; set; } = new List<Reservation>();
}
