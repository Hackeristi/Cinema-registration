using System.ServiceModel;
using Projekt1_Server.DTOs;

namespace Projekt1_Server;

[ServiceContract]
public interface ICinemaService
{
    [OperationContract]
    List<MovieDto> GetMovies();
    
    [OperationContract]
    MovieDetailsDto GetMovieDetails(int movieId);
    
    [OperationContract]
    List<ShowtimeDto> GetShowtimes(int movieId, DateTime date);
    
    [OperationContract]
    List<SeatDto> GetSeats(int filmshowId);

    [OperationContract]
    ReservationDto GetReservationDetails(int userId, int reservationId);

    [OperationContract]
    ReservationUpdateDto UpdateReservation(int userId, int reservationId, int newshowId, List<int> newseats);

    [OperationContract]
    ReservationCreateDto CreateReservation(int userId, int filmshowId, List<int> seats);

    [OperationContract]
    bool ReservationDelete(int  userId, int reservationId);
    
    [OperationContract]
    List<ReservationDto> GetUserReservations(int userId);

    [OperationContract]
    byte[] ReservationToPdf(int reservationId);
    
    [OperationContract]
    RegisterDto Register(string name, string surname,string email, string password, string confirmPassword);
    
    [OperationContract]
    UserLoginDto Login(string email, string password);

    [OperationContract]
    byte[] GetMoviePoster(int movieId);
}
