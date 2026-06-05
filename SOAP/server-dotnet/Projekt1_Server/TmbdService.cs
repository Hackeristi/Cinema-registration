using System.Text.Json;
using Microsoft.EntityFrameworkCore;
using Projekt1_Server.Models;

namespace Projekt1_Server;

public class TmdbService
{
    private readonly CinemaContext _context;
    private readonly HttpClient _httpClient;
    private const string ApiKey = "02265840166595b53033f66032f8c719"; 

    public TmdbService(CinemaContext context, HttpClient httpClient)
    {
        _context = context;
        _httpClient = httpClient;
    }

    public async Task SeedDatabaseAsync()
    {
        if (await _context.Movies.AnyAsync()) return;

        try
        {
            var listResponse = await _httpClient.GetStringAsync($"https://api.themoviedb.org/3/movie/popular?api_key={ApiKey}&language=pl-PL");
            using var listDoc = JsonDocument.Parse(listResponse);
            var moviesArray = listDoc.RootElement.GetProperty("results");
            
            var localActorsCache = new Dictionary<int, Actor>();

            foreach (var movieItem in moviesArray.EnumerateArray().Take(10))
            {
                int tmdbId = movieItem.GetProperty("id").GetInt32();

                var detailsResponse = await _httpClient.GetStringAsync($"https://api.themoviedb.org/3/movie/{tmdbId}?api_key={ApiKey}&language=pl-PL&append_to_response=credits");
                using var detailsDoc = JsonDocument.Parse(detailsResponse);
                var details = detailsDoc.RootElement;

                string title = details.GetProperty("title").GetString() ?? "Brak tytułu";
                string overview = details.GetProperty("overview").GetString() ?? "Brak opisu";
                if (overview.Length > 1000) overview = overview.Substring(0, 997) + "...";

                int duration = details.TryGetProperty("runtime", out var rt) && rt.ValueKind != JsonValueKind.Null ? rt.GetInt32() : 120;

                var genresList = details.GetProperty("genres").EnumerateArray();
                string category = string.Join(" / ", genresList.Select(g => g.GetProperty("name").GetString()));
                if (category.Length > 50) category = category.Substring(0, 47) + "...";

                string releaseDateStr = details.GetProperty("release_date").GetString();
                DateTime releaseDate = DateTime.TryParse(releaseDateStr, out var parsedDate) ? parsedDate : DateTime.Now;

                byte[] posterBytes = null;
                if (details.TryGetProperty("poster_path", out var pp) && pp.ValueKind != JsonValueKind.Null)
                {
                    string posterPath = pp.GetString();
                    posterBytes = await _httpClient.GetByteArrayAsync($"https://image.tmdb.org/t/p/w500{posterPath}");
                }

                string directorName = "Brak reżysera";
                var movieActorsList = new List<Actor>(); 

                if (details.TryGetProperty("credits", out var credits))
                {
                    if (credits.TryGetProperty("crew", out var crew))
                    {
                        var directorObj = crew.EnumerateArray().FirstOrDefault(c => c.GetProperty("job").GetString() == "Director");
                        if (directorObj.ValueKind != JsonValueKind.Undefined)
                        {
                            directorName = directorObj.GetProperty("name").GetString();
                        }
                    }

                    if (credits.TryGetProperty("cast", out var cast))
                    {
                        var topActors = cast.EnumerateArray().Take(5).ToList();

                        foreach (var actorNode in topActors)
                        {
                            string fullName = actorNode.GetProperty("name").GetString();
                            if (string.IsNullOrWhiteSpace(fullName)) continue;
                            
                            int tmdbActorId = actorNode.GetProperty("id").GetInt32();

                            var parts = fullName.Split(' ', 2);
                            string firstName = parts[0];
                            string lastName = parts.Length > 1 ? parts[1] : "";
                            
                            if (localActorsCache.TryGetValue(tmdbActorId, out var cachedActor))
                            {
                                movieActorsList.Add(cachedActor);
                            }
                            else
                            {
                                var existingActor = await _context.Actors
                                    .FirstOrDefaultAsync(a => a.ActorId == tmdbActorId);
                                
                                if (existingActor != null)
                                {
                                    localActorsCache[tmdbActorId] = existingActor; 
                                    movieActorsList.Add(existingActor);
                                }
                                else
                                {
                                    var newActor = new Actor 
                                    { 
                                        ActorId = tmdbActorId, 
                                        Name = firstName, 
                                        Surmane = lastName 
                                    };
                                    localActorsCache[tmdbActorId] = newActor; 
                                    movieActorsList.Add(newActor);
                                }
                            }
                        }
                    }
                }

                var movie = new Movie
                {
                    MovieId = tmdbId,
                    Title = title,
                    Description = overview,
                    Duration = duration,
                    Genre = category,
                    Premiere = releaseDate.Year,
                    Poster = posterBytes,
                    Director = directorName,
                    Actors = movieActorsList 
                };

                _context.Movies.Add(movie);
            }

            await _context.SaveChangesAsync();
            Console.WriteLine("Pomyślnie zasilono bazę danych filmami i aktorami z TMDB.");
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Błąd podczas pobierania danych z TMDB: {ex.Message}");
        }
    }
    
    public async Task GenerateScheduleAsync()
    {
        var movies = await _context.Movies.ToListAsync();
        if (!movies.Any())
        {
            Console.WriteLine("Brak filmów w bazie! Najpierw pobierz filmy.");
            return;
        }
        
        DateTime startGeneratingFromDate = DateTime.Today;

        var latestShow = await _context.FilmShows
            .OrderByDescending(fs => fs.ShowDatetime)
            .FirstOrDefaultAsync();

        if (latestShow != null && latestShow.ShowDatetime.Date >= DateTime.Today)
        {
            startGeneratingFromDate = latestShow.ShowDatetime.Date.AddDays(1);
        }
        
        DateTime targetEndDate = DateTime.Today.AddDays(7);
        
        if (startGeneratingFromDate >= targetEndDate)
        {
            Console.WriteLine("Harmonogram jest już pełny na co najmniej 7 dni w przód.");
            return;
        }
        
        int daysToGenerate = (targetEndDate - startGeneratingFromDate).Days;
        
        var screenIds = new List<int> { 1, 2, 3, 4, 5, 6 };
        var random = new Random();
        
        int currentShowId = 1;
        if (await _context.FilmShows.AnyAsync())
        {
            currentShowId = await _context.FilmShows.MaxAsync(fs => fs.FilmShowId) + 1;
        }
        
        Console.WriteLine($"Generowanie harmonogramu od {startGeneratingFromDate.ToShortDateString()} na {daysToGenerate} dni...");

        for (int dayOffset = 0; dayOffset < daysToGenerate; dayOffset++)
        {
            DateTime dailyStart = startGeneratingFromDate.AddDays(dayOffset).AddHours(10);

            var screenAvailableFrom = screenIds.ToDictionary(s => s, s => dailyStart);
            var movieAvailableFrom = movies.ToDictionary(m => m.MovieId, m => dailyStart);

            var showsToSchedule = new List<Movie>();
            for (int i = 0; i < 3; i++)
            {
                showsToSchedule.AddRange(movies);
            }
            
            showsToSchedule = showsToSchedule.OrderBy(x => random.Next()).ToList();

            foreach (var movie in showsToSchedule)
            {
                var earliestScreen = screenAvailableFrom.OrderBy(kvp => kvp.Value).First();
                int selectedScreenId = earliestScreen.Key;
                DateTime screenFreeTime = earliestScreen.Value;

                DateTime movieFreeTime = movieAvailableFrom[movie.MovieId];

                DateTime startTime = screenFreeTime > movieFreeTime ? screenFreeTime : movieFreeTime;

                int minutesRemainder = startTime.Minute % 5;
                if (minutesRemainder != 0)
                {
                    startTime = startTime.AddMinutes(5 - minutesRemainder);
                }
                
                var newShow = new FilmShow
                {
                    FilmShowId = currentShowId++,
                    MovieId = movie.MovieId,
                    ScreenId = selectedScreenId, 
                    ShowDatetime = startTime
                };
                _context.FilmShows.Add(newShow);
                
                DateTime endTime = startTime.AddMinutes(movie.Duration);
                DateTime cleaningEndTime = endTime.AddMinutes(30); 

                screenAvailableFrom[selectedScreenId] = cleaningEndTime; 
                movieAvailableFrom[movie.MovieId] = endTime; 
            }
        }

        await _context.SaveChangesAsync();
        Console.WriteLine($"Pomyślnie dopisano {daysToGenerate} dni do harmonogramu!");
    }
}