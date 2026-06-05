namespace Projekt1_Server.DTOs;

public class ReservationUpdateDto
{
    public int ReservationId { get; set; }
    public int MovieId { get; set; }
    public int NewFilmShowId { get; set; }
    public List<int> NewSeats { get; set; }
}