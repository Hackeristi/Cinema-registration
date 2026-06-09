using Microsoft.EntityFrameworkCore;
using System.Linq;
using Projekt1_Server.DTOs;
using Projekt1_Server.Models;
using QuestPDF.Fluent;
using QuestPDF.Helpers;
using QuestPDF.Infrastructure;

namespace Projekt1_Server;

public class CinemaService : ICinemaService
{
	private readonly CinemaContext _context;

	public CinemaService(CinemaContext context)
	{
		_context = context;
	}

	public List<MovieDto> GetMovies()
	{
		var moviesList = _context.FilmShows
			.Select(show => new MovieDto
			{
				MovieId = show.MovieId,
				ShowId = show.FilmShowId,
				Title = show.Movie.Title,
				Genre = show.Movie.Genre,
				ShowDatetime = show.ShowDatetime
			}).ToList();
		return moviesList;
	}

	public MovieDetailsDto GetMovieDetails(int movieId)
	{
		var movie = _context.Movies
			.Include(m => m.Actors)
			.FirstOrDefault(movie => movie.MovieId == movieId);
		
		if(movie==null)
		{
			throw new Exception("Nie istnieje film o takim id!");
		}

		var movieDetails = new MovieDetailsDto
		{
			MovieId = movie.MovieId,
			Title = movie.Title,
			Description = movie.Description,
			Director = movie.Director,
			Actors = movie.Actors
				.Select(a => $"{a.Name} {a.Surmane}")
				.ToList(),
			Duration = movie.Duration,
			Premiere = movie.Premiere,
			Poster = movie.Poster
		};
		return movieDetails;
	}

	public List<ShowtimeDto> GetShowtimes(int movieId, DateTime date)
	{
		var showtimesList = _context.FilmShows
			.Include(f=>f.Screen)
			.Where(f=>f.MovieId == movieId)
			.Where(f=> f.ShowDatetime.Date == date)
			.Select(f=> new ShowtimeDto
			{
				FilmShowId = f.FilmShowId,
				ScreenId = f.ScreenId,
				ShowDatetime = f.ShowDatetime
			})
			.OrderBy(f => f.ShowDatetime)
			.ToList();
		
		return showtimesList;
	}

	public List<SeatDto> GetSeats(int filmshowId)
	{
		var filmshow = _context.FilmShows.FirstOrDefault(f => f.FilmShowId == filmshowId);
		if (filmshow == null)
		{
			return new List<SeatDto>();
		}
		var seatsList = _context.Seats
			.Where(s => s.ScreenId == filmshow.ScreenId)
			.Select(s => new SeatDto
			{
				SeatId = s.SeatId,
				Number = s.Number,
				RowNum = s.RowNum,
				IsTaken = s.Reservations.Any(r=>r.FilmShowId==filmshow.FilmShowId),
			}).ToList();
		return seatsList;
	}

	public ReservationDto GetReservationDetails(int userId, int reservationId)
	{
		var reservation = _context.Reservations
			.Include(r=>r.Seats)
			.Include(r=>r.FilmShow)
				.ThenInclude(fs=>fs.Movie)
			.FirstOrDefault(r => r.ReservationId == reservationId 
			&& r.UsersId == userId);

		if (reservation == null)
		{
			throw new Exception("Nie istnieje rezerwacja to takim id!");
		}

		var reservationDetails = new ReservationDto
		{
			ReservationId = reservation.ReservationId,
			Title = reservation.FilmShow.Movie.Title,
			ShowDatetime = reservation.FilmShow.ShowDatetime,
			TakenSeats = reservation.Seats
				.Select(s => $"Rząd {s.RowNum}, Miejsce {s.Number}")
				.ToList()
		};
		return reservationDetails;
	}

	public ReservationCreateDto CreateReservation(int userId, int filmshowId, List<int> seats)
	{
		if (seats == null || seats.Count == 0)
		{
			throw new Exception("Nie wybrano żadnych miejsc!");
		}

		var newReservation = new Reservation
		{
			UsersId = userId,
			FilmShowId = filmshowId,
			Status = "Utworzona",
			ReservationDate = DateTime.Now
		};
		
		var takeSeats = _context.Seats.Where(s => seats.Contains(s.SeatId)).ToList();
		
		newReservation.Seats = takeSeats;
		_context.Reservations.Add(newReservation);
		_context.SaveChanges();
		
		return new ReservationCreateDto
		{
			ReservationId = newReservation.ReservationId,
			UserId = userId,
			FilmShowId = filmshowId,
			SelectedSeats = seats
		};
	}
	
	public ReservationUpdateDto UpdateReservation(int userId, int reservationId, int newshowId, List<int> newseats)
	{
		var reservation = _context.Reservations
			.Include(r => r.Seats)
			.Include(r => r.FilmShow)
			.FirstOrDefault(r => r.ReservationId == reservationId && r.UsersId == userId);

		if (reservation == null) throw new Exception("Nie istnieje rezerwacja o takim id!");
		
		if (DateTime.Now >= reservation.FilmShow.ShowDatetime)
		{
			throw new Exception("Nie można edytować rezerwacji na seans, który już się rozpoczął!");
		}
		
		var newShow = _context.FilmShows
			.Include(fs => fs.Movie)
			.FirstOrDefault(fs => fs.FilmShowId == newshowId);
        
		if (newShow == null) throw new Exception("Wybrany nowy seans nie istnieje!");
		
		reservation.Seats.Clear();
		var newSeatsList = _context.Seats.Where(s => newseats.Contains(s.SeatId)).ToList();
		if (newSeatsList.Count == 0) throw new Exception("Wybrane nowe miejsca nie istnieją!");
		
		reservation.FilmShow = newShow; 
		reservation.Seats = newSeatsList;
		reservation.Status = "Zmodyfikowana";
		reservation.ReservationDate = DateTime.Now;
    
		_context.SaveChanges();
		
		return new ReservationUpdateDto
		{
			ReservationId = reservation.ReservationId,
			MovieId = newShow.MovieId, 
			NewFilmShowId = newshowId,
			NewSeats = newseats
		};
	}
	
	
	public bool ReservationDelete(int userId, int reservationId)
	{
		
		var reservation = _context.Reservations
			.Include(r => r.FilmShow)
			.Include(r => r.Seats)
			.FirstOrDefault(r => r.ReservationId == reservationId && r.UsersId == userId);
		
		if (reservation == null)
		{
			throw new Exception("Nie istnieje rezerwacja o takim id lub nie masz do niej dostępu!");
		}
		
		if (DateTime.Now >= reservation.FilmShow.ShowDatetime)
		{
			throw new Exception("Nie można anulować rezerwacji na seans, który już się rozpoczął lub minął!");
		}
		reservation.Seats.Clear();
		_context.Reservations.Remove(reservation);
		_context.SaveChanges();

		return true;
	}
	
	public List<ReservationDto> GetUserReservations(int userId)
	{
		var reservations = _context.Reservations
			.Include(r => r.Seats)
			.Include(r => r.FilmShow)
				.ThenInclude(fs => fs.Movie)
			.Where(r => r.UsersId == userId)
			.ToList();

		return reservations.Select(reservation => new ReservationDto
		{
			ReservationId = reservation.ReservationId,
			Title = reservation.FilmShow.Movie.Title,
			ShowDatetime = reservation.FilmShow.ShowDatetime,
			FilmShowId = reservation.FilmShowId,
			TakenSeats = reservation.Seats
				.Select(s => $"Rząd {s.RowNum}, Miejsce {s.Number}")
				.ToList()
		}).ToList();
	}
	
	private byte[] GenerateReservationPdf(ReservationPDFDto details)
	{
	    QuestPDF.Settings.License = LicenseType.Community;
	    
	    var document = Document.Create(container =>
	    {
	        container.Page(page =>
	        {
	            page.Size(PageSizes.A5.Landscape());
	            page.Margin(1.5f, Unit.Centimetre);
	            page.PageColor(Colors.White);
	            page.DefaultTextStyle(x => x.FontSize(12).FontFamily(Fonts.Arial));
	            
	            page.Header()
	                .AlignCenter()
	                .Text("BILET WSTĘPU DO KINA")
	                .SemiBold().FontSize(24).FontColor(Colors.Blue.Darken3);
	            
	            page.Content()
	                .PaddingVertical(1, Unit.Centimetre)
	                .Column(column =>
	                {
	                    column.Spacing(8);

	                    column.Item().Text($"Nr rezerwacji: {details.ReservationId}").FontSize(10).FontColor(Colors.Grey.Medium);
	                    column.Item().Text($"Kupujący (Email): {details.Email}");
	                    
	                    column.Item().PaddingTop(10).Text($"Film: {details.Title}").Bold().FontSize(18);
	                    
	                    column.Item().Text(text =>
	                    {
	                        text.Span("Data i godzina: ").SemiBold();
	                        text.Span(details.ShowDatetime.ToString("dd.MM.yyyy, HH:mm"));
	                    });
	                    
	                    column.Item().Text($"Czas trwania: {details.Duration} min");
	                    column.Item().Text($"Sala nr: {details.ScreenId}");
	                    
	                    string seatsText = string.Join(", ", details.Seats);
	                    
	                    column.Item().PaddingTop(10).Background(Colors.Grey.Lighten3).Padding(5).Text(text =>
	                    {
	                        text.Span("Twoje miejsca: ").SemiBold();
	                        text.Span(seatsText).Bold().FontColor(Colors.Red.Medium);
	                    });
	                });
	            
	            page.Footer()
	                .AlignCenter()
	                .Text(x =>
	                {
	                    x.Span("Dziękujemy za wybór naszego kina! ");
	                    x.Span("Wygenerowano automatycznie.").FontSize(10).FontColor(Colors.Grey.Medium);
	                });
	        });
	    });
	    
	    return document.GeneratePdf();
	}

	public byte[] ReservationToPdf(int reservationId)
	{
		var reservation = _context.Reservations
			.Include(r=>r.Seats)
			.Include(r=>r.FilmShow)
				.ThenInclude(fs=>fs.Movie)
			.Include(r=>r.Users)
			.FirstOrDefault(r => r.ReservationId == reservationId);
		if (reservation == null)
		{
			throw new Exception("Rezerwacja nie istnieje!");
		}
		
		var pdfDto = new ReservationPDFDto
		{
			ReservationId = reservation.ReservationId,
			Email = reservation.Users.Email,
			Title = reservation.FilmShow.Movie.Title,
			ShowDatetime = reservation.FilmShow.ShowDatetime,
			Duration = reservation.FilmShow.Movie.Duration,
			ScreenId = reservation.FilmShow.ScreenId,
			Seats = reservation.Seats.Select(s => $"Rząd {s.RowNum}, M: {s.Number}").ToList()
		};
		
		return GenerateReservationPdf(pdfDto);
	}

	public RegisterDto Register(string name, string surname, string email, string password, string confirmPassword)
	{
	    try
	    {
	        if (string.IsNullOrWhiteSpace(name) || string.IsNullOrWhiteSpace(surname) || 
	            string.IsNullOrWhiteSpace(email) || string.IsNullOrWhiteSpace(password) || 
	            string.IsNullOrWhiteSpace(confirmPassword))
	        {
	            return new RegisterDto { UserId = -1, ErrorMessage = "Wszystkie pola są wymagane. Uzupełnij brakujące dane!" };
	        }
	        
	        if (password != confirmPassword)
	        {
	           return new RegisterDto { UserId = -1, ErrorMessage = "Podane hasła nie są identyczne!" };
	        }
	        
	        var existingUser = _context.Users.FirstOrDefault(u => u.Email == email);
	        if (existingUser != null)
	        {
	           return new RegisterDto { UserId = -1, ErrorMessage = "Konto z tym adresem email już istnieje!" };
	        }
	        
	        string hashedPassword = BCrypt.Net.BCrypt.HashPassword(password);
	        
	        var newUser = new User 
	        {
	           Name = name,
	           Surname = surname,
	           Email = email,
	           Password = hashedPassword 
	        };
	        
	        _context.Users.Add(newUser);
	        _context.SaveChanges();
	        
	        return new RegisterDto
	        {
	           UserId = newUser.UsersId, 
	           Name = newUser.Name,
	           Surname = newUser.Surname,
	           Email = newUser.Email,
	           ErrorMessage = null 
	        };
	    }
	    catch (Exception ex)
	    {
	        return new RegisterDto
	        {
	            UserId = -1,
	            ErrorMessage = "REGISTER ERROR: " + ex.Message
	        };
	    }
	}

	public UserLoginDto Login(string email, string password)
	{
	    try
	    {
	        if (string.IsNullOrWhiteSpace(email) || string.IsNullOrWhiteSpace(password))
	        {
	            return new UserLoginDto { ErrorMessage = "Email i hasło są wymagane!" };
	        }

	        var user = _context.Users.FirstOrDefault(u => u.Email == email);
	        
	        if (user == null)
	        {
	           return new UserLoginDto { ErrorMessage = "Nieprawidłowy adres email lub hasło." };
	        }
	        
	        bool isPasswordValid = BCrypt.Net.BCrypt.Verify(password, user.Password); 

	        if (!isPasswordValid)
	        {
	           return new UserLoginDto { ErrorMessage = "Nieprawidłowy adres email lub hasło." };
	        }
	        
	        return new UserLoginDto
	        { 
		        UserId = user.UsersId,
	           Email = user.Email,
	           UserName = user.Name,
	           ErrorMessage = null 
	        };
	    }
	    catch (Exception ex)
	    {
	        return new UserLoginDto
	        {
	            ErrorMessage = "LOGIN ERROR: " + ex.Message
	        };
	    }
	}
	
	public byte[] GetMoviePoster(int movieId)
	{
		var posterBytes = _context.Movies
			.Where(m => m.MovieId == movieId)
			.Select(m => m.Poster)
			.FirstOrDefault();

		if (posterBytes == null || posterBytes.Length == 0)
		{
			throw new Exception("Brak okładki dla tego filmu."); 
		}
		return posterBytes;
	}

	public MovieDto GetMovieDetailsFromReservation(int reservationId)
    {
    	var show = _context.Reservations
        	.Where(r => r.ReservationId == reservationId)
        	.Select(r => r.FilmShow)
        	.FirstOrDefault();

    	if (show == null || show.Movie == null)
    	{
        	throw new Exception($"Nie znaleziono szczegółów filmu dla rezerwacji o ID {reservationId}");
    	}
	
    	var movieDto = new MovieDto
    	{
			MovieId = show.Movie.MovieId, 
        	ShowId = show.FilmShowId,
        	Title = show.Movie.Title,
        	Genre = show.Movie.Genre,
        	ShowDatetime = show.ShowDatetime
    	};

   		return movieDto;
	}
}
