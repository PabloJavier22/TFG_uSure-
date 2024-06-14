using System.ComponentModel.DataAnnotations.Schema;
using System.ComponentModel.DataAnnotations;
using System.Text.Json.Serialization;

namespace uSure_server.Controllers
{
    [Table("grupoproducto")]
    public class GrupoProducto
    {
        [JsonIgnore]
        public Grupo Grupo { get; set; }

        [JsonIgnore]
        public Producto Producto { get; set; }

        [Key]
        [Column(Order = 1)]
        public int IDGrupo { get; set; }

        [Key]
        [Column(Order = 2)]
        public int IDProducto { get; set; }
    }
}
