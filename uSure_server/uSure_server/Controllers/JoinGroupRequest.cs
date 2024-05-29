using System.ComponentModel.DataAnnotations;

namespace uSure_server.Controllers
{
    public class JoinGroupRequest
    {
        public Guid UsuarioUID { get; set; }
        public string CodigoGrupo { get; set; }
    }
}   