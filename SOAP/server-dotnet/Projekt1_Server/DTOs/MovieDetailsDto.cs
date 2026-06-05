using Projekt1_Server.Models;
namespace Projekt1_Server.DTOs;

public class MovieDetailsDto
{
    public int MovieId { get; set; }
    public string Title { get; set; }
    public string Director { get; set; }
    public string Description { get; set; }
    public int Premiere { get; set; }
    public int Duration { get; set; }
    public byte[] Poster { get; set; }
    public List<string> Actors { get; set; }
}