using Microsoft.EntityFrameworkCore;
using Projekt1_Server;
using Projekt1_Server.Models;
using SoapCore;
using System.ServiceModel;


var builder = WebApplication.CreateBuilder(args);
var connectionString = builder.Configuration.GetConnectionString("DefaultConnection");

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

builder.Services.AddDbContext<CinemaContext>(options =>
    options.UseSqlServer(connectionString));

builder.Services.AddScoped<ICinemaService, CinemaService>();

builder.Services.AddSoapCore();

var app = builder.Build();
app.UseRouting();

app.UseEndpoints(endpoints =>
{
    endpoints.UseSoapEndpoint<ICinemaService>("/CinemaService.asmx", new SoapEncoderOptions
    {
        MessageVersion = System.ServiceModel.Channels.MessageVersion.Soap11
    }, SoapSerializer.DataContractSerializer);
});

using (var scope = app.Services.CreateScope())
{
    var services = scope.ServiceProvider;
    var context = services.GetRequiredService<CinemaContext>();
    
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

app.MapControllers();

app.Run();
