using System.ComponentModel.DataAnnotations.Schema;
using System.ComponentModel.DataAnnotations;

namespace uSure_server.Controllers
{
    public class GrupoProducto
    {
        public Grupo Grupo { get; set; }
        public Producto Producto { get; set; }

        [Key]
        [Column(Order = 1)]
        public int ID_Grupo { get; set; }

        [Key]
        [Column(Order = 2)]
        public int ID_Producto { get; set; }
    }
}