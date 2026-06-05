namespace Projekt1_Server.DTOs;

public class SeatDto
{
    public int SeatId { get; set; }
    public int Number { get; set; }
    public int RowNum { get; set; }
    public bool IsTaken { get; set; }
}