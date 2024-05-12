using System.ComponentModel.DataAnnotations;

namespace uSure_server.Controllers
{
    public class Producto
    {
        [Key]
        public int ID { get; set; }
        public string Nombre { get; set; }
        public string Descripcion { get; set; }
        public int Cantidad { get; set; }
        public int IDCategoria { get; set; }
        public Categoria Categoria { get; set; }
    }
}