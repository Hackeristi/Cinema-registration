namespace Projekt1_Server.DTOs;

public class ReservationCreateDto
{
    public int ReservationId { get; set; }
    public int UserId { get; set; }
    public int FilmShowId { get; set; }
    public List<int> SelectedSeats { get; set; }
}