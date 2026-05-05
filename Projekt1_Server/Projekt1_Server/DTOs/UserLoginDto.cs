namespace Projekt1_Server.DTOs;

public class UserLoginDto
{
    public int UserId { get; set; }
    public string UserName { get; set; }
    public string Email { get; set; }
    public string Password { get; set; }
}