using System.ComponentModel.DataAnnotations;

namespace uSure_server.Controllers
{
    public class LoginRequest
    {
        public string Nombre { get; set; }
        public string Password { get; set; }
    }

}