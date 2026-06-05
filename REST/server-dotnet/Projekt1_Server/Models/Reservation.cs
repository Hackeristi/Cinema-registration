using System;
using System.Collections.Generic;

namespace Projekt1_Server.Models;

public partial class Reservation
{
    public int ReservationId { get; set; }

    public int FilmShowId { get; set; }

    public int UsersId { get; set; }

    public string Status { get; set; }

    public DateTime ReservationDate { get; set; }

    public virtual FilmShow? FilmShow { get; set; }

    public virtual User? Users { get; set; }

    public virtual ICollection<Seat> Seats { get; set; } = new List<Seat>();
}
