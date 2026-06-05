using System;
using System.Collections.Generic;

namespace Projekt1_Server.Models;

public partial class User
{
    public int UsersId { get; set; }

    public string Name { get; set; }

    public string Surname { get; set; }

    public string Email { get; set; }
    
    public string Password { get; set; } = null!;

    public virtual ICollection<Reservation> Reservations { get; set; } = new List<Reservation>();
}
