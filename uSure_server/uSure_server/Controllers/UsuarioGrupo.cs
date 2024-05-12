using System;
using System.ComponentModel.DataAnnotations;

namespace uSure_server.Controllers
{
    public class UsuarioGrupo
    {
        [Key]
        public Guid UsuarioUID { get; set; }
        public Usuario Usuario { get; set; }

        public int GrupoId { get; set; }
        public Grupo Grupo { get; set; }
    }
}
