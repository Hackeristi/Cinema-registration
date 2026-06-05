using System;
using System.Collections.Generic;
using Microsoft.EntityFrameworkCore;

namespace Projekt1_Server.Models;

public partial class CinemaContext : DbContext
{
    public CinemaContext()
    {
    }

    public CinemaContext(DbContextOptions<CinemaContext> options)
        : base(options)
    {
    }

    public virtual DbSet<Actor> Actors { get; set; }

    public virtual DbSet<FilmShow> FilmShows { get; set; }

    public virtual DbSet<Movie> Movies { get; set; }

    public virtual DbSet<Reservation> Reservations { get; set; }

    public virtual DbSet<Screen> Screens { get; set; }

    public virtual DbSet<Seat> Seats { get; set; }

    public virtual DbSet<User> Users { get; set; }

    protected override void OnConfiguring(DbContextOptionsBuilder optionsBuilder)
#warning To protect potentially sensitive information in your connection string, you should move it out of source code. You can avoid scaffolding the connection string by using the Name= syntax to read it from configuration - see https://go.microsoft.com/fwlink/?linkid=2131148. For more guidance on storing connection strings, see https://go.microsoft.com/fwlink/?LinkId=723263.
        => optionsBuilder.UseSqlServer("Server=(localdb)\\mssqllocaldb;Database=CinemaDB;Trusted_Connection=True;TrustServerCertificate=True;");

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Actor>(entity =>
        {
            entity.HasKey(e => e.ActorId).HasName("PK__Actors__8B2447B41E1CDAE3");

            entity.Property(e => e.ActorId)
                .ValueGeneratedNever()
                .HasColumnName("actor_id");
            entity.Property(e => e.Name)
                .HasMaxLength(20)
                .HasColumnName("name");
            entity.Property(e => e.Surmane)
                .HasMaxLength(30)
                .HasColumnName("surmane");

            entity.HasMany(d => d.Movies).WithMany(p => p.Actors)
                .UsingEntity<Dictionary<string, object>>(
                    "ActorMovie",
                    r => r.HasOne<Movie>().WithMany()
                        .HasForeignKey("MovieId")
                        .OnDelete(DeleteBehavior.ClientSetNull)
                        .HasConstraintName("FK__Actor_Mov__movie__286302EC"),
                    l => l.HasOne<Actor>().WithMany()
                        .HasForeignKey("ActorId")
                        .OnDelete(DeleteBehavior.ClientSetNull)
                        .HasConstraintName("FK__Actor_Mov__actor__29572725"),
                    j =>
                    {
                        j.HasKey("ActorId", "MovieId").HasName("PK__Actor_Mo__031898C040923F7F");
                        j.ToTable("Actor_Movie");
                        j.IndexerProperty<int>("ActorId").HasColumnName("actor_id");
                        j.IndexerProperty<int>("MovieId").HasColumnName("movie_id");
                    });
        });

        modelBuilder.Entity<FilmShow>(entity =>
        {
            entity.HasKey(e => e.FilmShowId).HasName("PK__Film_sho__E447150F91CE3070");

            entity.ToTable("Film_shows");

            entity.Property(e => e.FilmShowId)
                .ValueGeneratedNever()
                .HasColumnName("film_show_id");
            entity.Property(e => e.MovieId).HasColumnName("movie_id");
            entity.Property(e => e.ScreenId).HasColumnName("screen_id");
            entity.Property(e => e.ShowDatetime)
                .HasColumnType("datetime")
                .HasColumnName("show_datetime");

            entity.HasOne(d => d.Movie).WithMany(p => p.FilmShows)
                .HasForeignKey(d => d.MovieId)
                .HasConstraintName("FK__Film_show__movie__403A8C7D");

            entity.HasOne(d => d.Screen).WithMany(p => p.FilmShows)
                .HasForeignKey(d => d.ScreenId)
                .HasConstraintName("FK__Film_show__scree__412EB0B6");
        });

        modelBuilder.Entity<Movie>(entity =>
        {
            entity.HasKey(e => e.MovieId).HasName("PK__Movies__83CDF74975B99564");

            entity.Property(e => e.MovieId)
                .ValueGeneratedNever()
                .HasColumnName("movie_id");
            entity.Property(e => e.Description).HasColumnName("description");
            entity.Property(e => e.Director)
                .HasMaxLength(50)
                .HasColumnName("director");
            entity.Property(e => e.Duration).HasColumnName("duration");
            entity.Property(e => e.Genre)
                .HasMaxLength(100)
                .HasColumnName("genre");
            entity.Property(e => e.Poster).HasColumnName("poster");
            entity.Property(e => e.Premiere).HasColumnName("premiere");
            entity.Property(e => e.Title)
                .HasMaxLength(80)
                .HasColumnName("title");
        });

        modelBuilder.Entity<Reservation>(entity =>
        {
            entity.HasKey(e => e.ReservationId).HasName("PK__Reservat__31384C29AFB94EFF");

            entity.Property(e => e.ReservationId)
                .HasDefaultValueSql("(NEXT VALUE FOR [ReservationSequence])")
                .HasColumnName("reservation_id");
            entity.Property(e => e.FilmShowId).HasColumnName("film_show_id");
            entity.Property(e => e.ReservationDate)
                .HasColumnType("datetime")
                .HasColumnName("reservation_date");
            entity.Property(e => e.Status)
                .HasMaxLength(50)
                .HasColumnName("status");
            entity.Property(e => e.UsersId).HasColumnName("users_id");

            entity.HasOne(d => d.FilmShow).WithMany(p => p.Reservations)
                .HasForeignKey(d => d.FilmShowId)
                .HasConstraintName("FK__Reservati__film___440B1D61");

            entity.HasOne(d => d.Users).WithMany(p => p.Reservations)
                .HasForeignKey(d => d.UsersId)
                .HasConstraintName("FK__Reservati__users__44FF419A");

            entity.HasMany(d => d.Seats).WithMany(p => p.Reservations)
                .UsingEntity<Dictionary<string, object>>(
                    "ReservationSeat",
                    r => r.HasOne<Seat>().WithMany()
                        .HasForeignKey("SeatId")
                        .OnDelete(DeleteBehavior.ClientSetNull)
                        .HasConstraintName("FK__Reservati__seat___48CFD27E"),
                    l => l.HasOne<Reservation>().WithMany()
                        .HasForeignKey("ReservationId")
                        .OnDelete(DeleteBehavior.ClientSetNull)
                        .HasConstraintName("FK__Reservati__reser__47DBAE45"),
                    j =>
                    {
                        j.HasKey("ReservationId", "SeatId").HasName("PK__Reservat__E83E92F03320E74F");
                        j.ToTable("Reservation_seats");
                        j.IndexerProperty<int>("ReservationId").HasColumnName("reservation_id");
                        j.IndexerProperty<int>("SeatId").HasColumnName("seat_id");
                    });
        });

        modelBuilder.Entity<Screen>(entity =>
        {
            entity.HasKey(e => e.ScreenId).HasName("PK__Screens__CC19B67AA6DAA23D");

            entity.Property(e => e.ScreenId)
                .ValueGeneratedNever()
                .HasColumnName("screen_id");
            entity.Property(e => e.Number).HasColumnName("number");
        });

        modelBuilder.Entity<Seat>(entity =>
        {
            entity.HasKey(e => e.SeatId).HasName("PK__Seats__906DED9CB36BA436");

            entity.Property(e => e.SeatId)
                .ValueGeneratedNever()
                .HasColumnName("seat_id");
            entity.Property(e => e.IsTaken).HasColumnName("is_taken");
            entity.Property(e => e.Number).HasColumnName("number");
            entity.Property(e => e.RowNum).HasColumnName("row_num");
            entity.Property(e => e.ScreenId).HasColumnName("screen_id");

            entity.HasOne(d => d.Screen).WithMany(p => p.Seats)
                .HasForeignKey(d => d.ScreenId)
                .HasConstraintName("FK__Seats__screen_id__2E1BDC42");
        });

        modelBuilder.Entity<User>(entity =>
        {
            entity.HasKey(e => e.UsersId).HasName("PK__Users__EAA7D14B361FEF71");

            entity.Property(e => e.UsersId)
                .HasDefaultValueSql("(NEXT VALUE FOR [UserSequence])")
                .HasColumnName("users_id");
            entity.Property(e => e.Email)
                .HasMaxLength(50)
                .HasColumnName("email");
            entity.Property(e => e.Name)
                .HasMaxLength(20)
                .HasColumnName("name");
            entity.Property(e => e.Password)
                .HasMaxLength(255)
                .HasColumnName("password");
            entity.Property(e => e.Surname)
                .HasMaxLength(30)
                .HasColumnName("surname");
        });
        modelBuilder.HasSequence("ReservationSequence");
        modelBuilder.HasSequence("UserSequence").StartsAt(2L);

        OnModelCreatingPartial(modelBuilder);
    }

    partial void OnModelCreatingPartial(ModelBuilder modelBuilder);
}
