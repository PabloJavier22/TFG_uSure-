using System;
using System.ComponentModel.DataAnnotations;
using System.Text.Json.Serialization;

namespace uSure_server.Controllers
{
    public class UsuarioGrupo
    {
        [Key]
        public Guid UsuarioUID { get; set; }

        [JsonIgnore]
        public Usuario Usuario { get; set; }

        public int GrupoId { get; set; }

        [JsonIgnore]
        public Grupo Grupo { get; set; }
    }
}
