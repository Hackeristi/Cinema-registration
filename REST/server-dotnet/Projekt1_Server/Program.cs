using Microsoft.EntityFrameworkCore;
using Projekt1_Server;
using Projekt1_Server.Models;
using Projekt1_Server.Hubs; 
using Microsoft.AspNetCore.RateLimiting;
using System.Threading.RateLimiting;

var builder = WebApplication.CreateBuilder(args);

var connectionString = Environment.GetEnvironmentVariable("DB_CONNECTION_STRING") 
                       ?? builder.Configuration.GetConnectionString("DefaultConnection");

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

builder.Services.AddDbContext<CinemaContext>(options =>
    options.UseSqlServer(connectionString));

builder.Services.AddScoped<ICinemaService, CinemaService>();

builder.Services.AddSignalR();

builder.Services.AddRateLimiter(options =>
{
    options.AddFixedWindowLimiter("DemoLimit", opt =>
    {
        opt.PermitLimit = 5; 
        opt.Window = TimeSpan.FromSeconds(10); 
        opt.QueueProcessingOrder = QueueProcessingOrder.OldestFirst;
        opt.QueueLimit = 0; 
    });
    options.RejectionStatusCode = StatusCodes.Status429TooManyRequests;
});

var app = builder.Build();

using (var scope = app.Services.CreateScope())
{
    var services = scope.ServiceProvider;
    var context = services.GetRequiredService<CinemaContext>();
    
    Console.WriteLine("Tworzenie struktury bazy danych...");
    try
    {
        context.Database.EnsureCreated();
        Console.WriteLine("Baza gotowa!");
    }
    catch (Exception ex)
    {
        Console.WriteLine("Baza już istnieje (załadowana z wolumenu na dysku). Pracuję dalej!");
    }
    
    var tmdb = new TmdbService(context, new HttpClient());
    await tmdb.SeedDatabaseAsync();
    await tmdb.GenerateScheduleAsync();
}

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI(options => 
    {
        options.SwaggerEndpoint("/swagger/v1/swagger.json", "Cinema API v1");
        options.RoutePrefix = string.Empty; 
    });
}

app.UseRateLimiter();
app.MapControllers().RequireRateLimiting("DemoLimit");

app.MapHub<CinemaHub>("/cinema-ws");

app.Run();