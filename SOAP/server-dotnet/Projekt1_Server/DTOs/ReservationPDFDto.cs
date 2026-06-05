namespace Projekt1_Server.DTOs;

public class ReservationPDFDto
{
    public string Email { get; set; }
    public int ReservationId { get; set; }
    public string Title { get; set; }
    public DateTime ShowDatetime { get; set; }
    public int Duration { get; set; }
    public int ScreenId { get; set; }
    public List<string> Seats { get; set; }
}