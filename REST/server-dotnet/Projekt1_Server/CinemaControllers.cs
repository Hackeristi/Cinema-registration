using Microsoft.AspNetCore.Mvc;
using Projekt1_Server.DTOs;

namespace Projekt1_Server;

[ApiController]
[Route("api")]
public class CinemaController : ControllerBase
{
    private readonly ICinemaService _cinemaService;
    
    public CinemaController(ICinemaService cinemaService)
    {
        _cinemaService = cinemaService;
    }
    

    [HttpGet("movies")]
    public IActionResult GetMovies()
    {
        return Ok(_cinemaService.GetMovies());
    }

    [HttpGet("movies/{movieId}")]
    public IActionResult GetMovieDetails(int movieId)
    {
        try
        {
            return Ok(_cinemaService.GetMovieDetails(movieId));
        }
        catch (Exception ex)
        {
            return NotFound(new { message = ex.Message });
        }
    }

    [HttpGet("movies/{movieId}/poster")]
    public IActionResult GetMoviePoster(int movieId)
    {
        try
        {
            var posterBytes = _cinemaService.GetMoviePoster(movieId);
            return File(posterBytes, "image/jpeg");
        }
        catch (Exception ex)
        {
            return NotFound(new { message = ex.Message });
        }
    }
    

    [HttpGet("movies/{movieId}/showtimes")]
    public IActionResult GetShowtimes(int movieId, [FromQuery] DateTime date)
    {
        return Ok(_cinemaService.GetShowtimes(movieId, date));
    }

    [HttpGet("showtimes/{filmshowId}/seats")]
    public IActionResult GetSeats(int filmshowId)
    {
        return Ok(_cinemaService.GetSeats(filmshowId));
    }
    

    [HttpGet("users/{userId}/reservations")]
    public IActionResult GetUserReservations(int userId)
    {
        return Ok(_cinemaService.GetUserReservations(userId));
    }

    [HttpGet("users/{userId}/reservations/{reservationId}")]
    public IActionResult GetReservationDetails(int userId, int reservationId)
    {
        try
        {
            return Ok(_cinemaService.GetReservationDetails(userId, reservationId));
        }
        catch (Exception ex)
        {
            return NotFound(new { message = ex.Message });
        }
    }
    
    [HttpPost("users/{userId}/reservations")]
    public IActionResult CreateReservation(int userId, [FromBody] ReservationCreateDto dto)
    {
        try
        {
            var result = _cinemaService.CreateReservation(userId, dto.FilmShowId, dto.SelectedSeats);
            return Ok(result);
        }
        catch (Exception ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }
    
    [HttpPut("users/{userId}/reservations/{reservationId}")]
    public IActionResult UpdateReservation(int userId, int reservationId, [FromBody] ReservationUpdateDto dto)
    {
        try
        {
            var result = _cinemaService.UpdateReservation(userId, reservationId, dto.NewFilmShowId, dto.NewSeats);
            return Ok(result);
        }
        catch (Exception ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }

    [HttpDelete("users/{userId}/reservations/{reservationId}")]
    public IActionResult DeleteReservation(int userId, int reservationId)
    {
        try
        {
            _cinemaService.ReservationDelete(userId, reservationId);
            return Ok(new { message = "Rezerwacja została pomyślnie usunięta." });
        }
        catch (Exception ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }

    [HttpGet("reservations/{reservationId}/pdf")]
    public IActionResult GetReservationPdf(int reservationId)
    {
        try
        {
            var pdfBytes = _cinemaService.ReservationToPdf(reservationId);
            return File(pdfBytes, "application/pdf", $"Bilet_{reservationId}.pdf");
        }
        catch (Exception ex)
        {
            return NotFound(new { message = ex.Message });
        }
    }
    
    [HttpPost("auth/register")]
    public IActionResult Register([FromBody] RegisterDto dto)
    {
        var result = _cinemaService.Register(dto.Name, dto.Surname, dto.Email, dto.Password, dto.ConfirmPassword);
        
        if (result.ErrorMessage != null)
        {
            return BadRequest(new { message = result.ErrorMessage });
        }
        
        return Ok(result);
    }
    
    [HttpPost("auth/login")]
    public IActionResult Login([FromBody] UserLoginDto dto)
    {
        var result = _cinemaService.Login(dto.Email, dto.Password);
        
        if (result.ErrorMessage != null)
        {
            return Unauthorized(new { message = result.ErrorMessage });
        }
        
        return Ok(result);
    }
}