using System;
using System.Collections.Generic;

namespace Projekt1_Server.Models;

public partial class Movie
{
    public int MovieId { get; set; }

    public string Title { get; set; }

    public string Director { get; set; }

    public string Description { get; set; }

    public string Genre { get; set; }

    public int Premiere { get; set; }

    public int Duration { get; set; }

    public byte[] Poster { get; set; }

    public virtual ICollection<FilmShow> FilmShows { get; set; } = new List<FilmShow>();

    public virtual ICollection<Actor> Actors { get; set; } = new List<Actor>();
}
