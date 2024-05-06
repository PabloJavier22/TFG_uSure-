using System.ComponentModel.DataAnnotations;
using System.Text.RegularExpressions;

namespace uSure_server.Controllers
{
    public class Categoria
    {
        [Key]
        public int ID { get; set; }
        public string Nombre { get; set; }
        public int ID_Grupo { get; set; }
        public Grupo Grupo { get; set; }

        public ICollection<Producto> Productos { get; set; }
    }
}