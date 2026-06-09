using Projekt1_Server.DTOs;

namespace Projekt1_Server;

public interface ICinemaService
{
    List<MovieDto> GetMovies();
    
    MovieDetailsDto GetMovieDetails(int movieId);
    
    List<ShowtimeDto> GetShowtimes(int movieId, DateTime date);
    
    List<SeatDto> GetSeats(int filmshowId);

    ReservationDto GetReservationDetails(int userId, int reservationId);

    ReservationUpdateDto UpdateReservation(int userId, int reservationId, int newshowId, List<int> newseats);

    ReservationCreateDto CreateReservation(int userId, int filmshowId, List<int> seats);

    bool ReservationDelete(int  userId, int reservationId);
    
    List<ReservationDto> GetUserReservations(int userId);

    byte[] ReservationToPdf(int reservationId);
    
    RegisterDto Register(string name, string surname,string email, string password, string confirmPassword);
    
    UserLoginDto Login(string email, string password);

    byte[] GetMoviePoster(int movieId);

	MovieDto GetMovieDetailsFromReservation(int reservationId);
}
